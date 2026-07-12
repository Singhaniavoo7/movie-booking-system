package com.moviebooking.service;

import com.moviebooking.entity.ShowSeat;
import com.moviebooking.entity.ShowSeatStatus;
import com.moviebooking.entity.User;
import com.moviebooking.exception.SeatUnavailableException;
import com.moviebooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Owns the seat-hold hot path. The critical section is intentionally small: lock
 * exactly the rows the caller asked for (in a stable, sorted order to avoid
 * deadlocks between two requests that hold overlapping-but-different seat sets),
 * validate them, flip their status, and commit -- releasing the DB row locks as
 * fast as possible so other requests for *different* seats are never blocked.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final ShowSeatRepository showSeatRepository;

    @Value("${app.booking.hold-duration-minutes}")
    private long holdDurationMinutes;

    @Transactional
    public List<ShowSeat> holdSeats(Long showId, List<Long> showSeatIds, User user) {
        // Sorting the lock order is what prevents deadlocks: if two transactions
        // both try to lock seats {1,2} and {2,1} concurrently, without a stable
        // order they could each hold one row and wait on the other forever.
        List<Long> sortedIds = showSeatIds.stream().sorted().distinct().toList();
        List<ShowSeat> locked = showSeatRepository.lockForUpdate(sortedIds);

        if (locked.size() != sortedIds.size()) {
            throw new SeatUnavailableException("One or more selected seats do not exist");
        }

        Instant now = Instant.now();
        for (ShowSeat seat : locked) {
            if (!seat.getShow().getId().equals(showId)) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " does not belong to show " + showId);
            }
            boolean effectivelyAvailable = seat.getStatus() == ShowSeatStatus.AVAILABLE || seat.isHoldExpired(now);
            if (!effectivelyAvailable) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getSeatTemplate().getRowLabel() + seat.getSeatTemplate().getSeatNumber()
                                + " is no longer available");
            }
        }

        Instant expiresAt = now.plusSeconds(holdDurationMinutes * 60);
        for (ShowSeat seat : locked) {
            seat.setStatus(ShowSeatStatus.HELD);
            seat.setHeldByUser(user);
            seat.setHoldExpiresAt(expiresAt);
        }
        showSeatRepository.saveAll(locked);
        log.info("Held {} seat(s) for user {} on show {} until {}", locked.size(), user.getEmail(), showId, expiresAt);
        return locked;
    }

    @Transactional
    public void releaseSeats(List<Long> showSeatIds, User user) {
        List<Long> sortedIds = showSeatIds.stream().sorted().distinct().toList();
        List<ShowSeat> locked = showSeatRepository.lockForUpdate(sortedIds);
        for (ShowSeat seat : locked) {
            if (seat.getStatus() == ShowSeatStatus.HELD
                    && seat.getHeldByUser() != null
                    && seat.getHeldByUser().getId().equals(user.getId())) {
                clearHold(seat);
            }
        }
        showSeatRepository.saveAll(locked);
    }

    /**
     * Validates that the given seats are currently HELD by {@code user} and not
     * expired, ready to be converted into a booking. Caller (BookingService) already
     * holds the row lock from within the same transaction.
     */
    public void assertHeldByUser(List<ShowSeat> seats, User user, Instant now) {
        for (ShowSeat seat : seats) {
            if (seat.getStatus() != ShowSeatStatus.HELD
                    || seat.isHoldExpired(now)
                    || seat.getHeldByUser() == null
                    || !seat.getHeldByUser().getId().equals(user.getId())) {
                throw new SeatUnavailableException(
                        "Your hold on seat " + seat.getSeatTemplate().getRowLabel() + seat.getSeatTemplate().getSeatNumber()
                                + " has expired or is invalid. Please reselect your seats.");
            }
        }
    }

    private void clearHold(ShowSeat seat) {
        seat.setStatus(ShowSeatStatus.AVAILABLE);
        seat.setHeldByUser(null);
        seat.setHoldExpiresAt(null);
    }
}
