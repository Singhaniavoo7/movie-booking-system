package com.moviebooking.service;

import com.moviebooking.dto.request.ShowRequest;
import com.moviebooking.entity.*;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final MovieService movieService;
    private final ScreenService screenService;
    private final PricingTierService pricingTierService;

    @Transactional
    public Show create(ShowRequest req) {
        Movie movie = movieService.get(req.getMovieId());
        Screen screen = screenService.get(req.getScreenId());
        PricingTier tier = pricingTierService.get(req.getPricingTierId());

        Instant startTime = req.getStartTime();
        Instant endTime = startTime.plus(Duration.ofMinutes(movie.getDurationMinutes()));

        assertNoOverlap(screen.getId(), startTime, endTime);

        Show show = Show.builder()
                .movie(movie).screen(screen).pricingTier(tier)
                .startTime(startTime).endTime(endTime).cancelled(false)
                .build();
        show = showRepository.save(show);

        // Materialize one ShowSeat per physical seat, snapshotting today's price so
        // later pricing-tier edits never retroactively change an already-scheduled show.
        List<ShowSeat> showSeats = screen.getSeatTemplates().stream()
                .map(template -> ShowSeat.builder()
                        .show(show)
                        .seatTemplate(template)
                        .status(ShowSeatStatus.AVAILABLE)
                        .price(resolvePrice(tier, template.getSeatType()))
                        .build())
                .toList();
        showSeatRepository.saveAll(showSeats);

        return show;
    }

    public List<Show> search(Long cityId, Long movieId, Instant from, Instant to) {
        Instant effectiveFrom = from != null ? from : Instant.now();
        Instant effectiveTo = to != null ? to : effectiveFrom.plus(30, ChronoUnit.DAYS);
        return showRepository.search(cityId, movieId, effectiveFrom, effectiveTo);
    }

    public Show get(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + id));
    }

    private BigDecimal resolvePrice(PricingTier tier, SeatType seatType) {
        BigDecimal price = tier.getSeatTypePrices().get(seatType);
        if (price == null) {
            throw new IllegalArgumentException(
                    "Pricing tier '" + tier.getName() + "' has no price configured for seat type " + seatType);
        }
        return price;
    }

    /** Prevents double-booking a screen for overlapping time windows. */
    private void assertNoOverlap(Long screenId, Instant startTime, Instant endTime) {
        boolean overlaps = showRepository.search(null, null, startTime.minus(6, ChronoUnit.HOURS), endTime.plus(6, ChronoUnit.HOURS))
                .stream()
                .filter(s -> s.getScreen().getId().equals(screenId))
                .anyMatch(s -> startTime.isBefore(s.getEndTime()) && endTime.isAfter(s.getStartTime()));
        if (overlaps) {
            throw new IllegalArgumentException("Screen already has an overlapping show in this time window");
        }
    }
}
