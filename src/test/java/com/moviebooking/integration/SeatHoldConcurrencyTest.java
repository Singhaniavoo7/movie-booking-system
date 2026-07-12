package com.moviebooking.integration;

import com.moviebooking.dto.request.*;
import com.moviebooking.entity.*;
import com.moviebooking.exception.SeatUnavailableException;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the core correctness requirement from the assignment: "multiple users may
 * attempt to book the same seat at the same time, and the system must correctly
 * serialize bookings without double-allocation." Fires N real concurrent threads at
 * SeatHoldService.holdSeats for the exact same seat and asserts exactly one wins.
 */
@SpringBootTest
@ActiveProfiles("test")
class SeatHoldConcurrencyTest {

    @Autowired private CityService cityService;
    @Autowired private TheaterService theaterService;
    @Autowired private ScreenService screenService;
    @Autowired private MovieService movieService;
    @Autowired private PricingTierService pricingTierService;
    @Autowired private ShowService showService;
    @Autowired private SeatHoldService seatHoldService;
    @Autowired private com.moviebooking.repository.ShowSeatRepository showSeatRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void onlyOneThreadWinsTheRaceForTheSameSeat() throws Exception {
        CityRequest cityReq = new CityRequest();
        cityReq.setName("ConcCity-" + System.nanoTime());
        cityReq.setState("ST");
        City city = cityService.create(cityReq);

        TheaterRequest theaterReq = new TheaterRequest();
        theaterReq.setName("Conc Theater");
        theaterReq.setAddress("Addr");
        theaterReq.setCityId(city.getId());
        Theater theater = theaterService.create(theaterReq);

        SeatRowRequest row = new SeatRowRequest();
        row.setRowLabel("A");
        row.setSeatCount(1); // exactly ONE seat -- everyone is racing for it
        row.setSeatType(SeatType.REGULAR);
        ScreenRequest screenReq = new ScreenRequest();
        screenReq.setName("Screen X");
        screenReq.setTheaterId(theater.getId());
        screenReq.setSeatLayout(List.of(row));
        Screen screen = screenService.create(screenReq);

        MovieRequest movieReq = new MovieRequest();
        movieReq.setTitle("Race Condition: The Movie");
        movieReq.setDurationMinutes(100);
        Movie movie = movieService.create(movieReq);

        PricingTierRequest tierReq = new PricingTierRequest();
        tierReq.setName("ConcTier-" + System.nanoTime());
        tierReq.setSeatTypePrices(Map.of(SeatType.REGULAR, java.math.BigDecimal.valueOf(100)));
        PricingTier tier = pricingTierService.create(tierReq);

        ShowRequest showReq = new ShowRequest();
        showReq.setMovieId(movie.getId());
        showReq.setScreenId(screen.getId());
        showReq.setPricingTierId(tier.getId());
        showReq.setStartTime(Instant.now().plus(5, ChronoUnit.DAYS));
        Show show = showService.create(showReq);

        Long seatId = showSeatRepository.findByShowId(show.getId()).get(0).getId();

        int threadCount = 20;
        List<User> users = java.util.stream.IntStream.range(0, threadCount).mapToObj(i -> userRepository.save(User.builder()
                        .name("Racer " + i)
                        .email("racer-" + i + "-" + System.nanoTime() + "@example.com")
                        .passwordHash(passwordEncoder.encode("password"))
                        .role(Role.CUSTOMER)
                        .enabled(true)
                        .build()))
                .toList();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);

        List<Future<?>> futures = users.stream().map(user -> pool.submit(() -> {
            try {
                startLine.await();
                seatHoldService.holdSeats(show.getId(), List.of(seatId), user);
                successes.incrementAndGet();
            } catch (SeatUnavailableException expected) {
                conflicts.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        })).toList();

        startLine.countDown(); // release all threads at once
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(threadCount - 1);

        ShowSeat finalState = showSeatRepository.findById(seatId).orElseThrow();
        assertThat(finalState.getStatus()).isEqualTo(ShowSeatStatus.HELD);
    }
}
