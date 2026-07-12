package com.moviebooking.scheduler;

import com.moviebooking.entity.ShowSeat;
import com.moviebooking.entity.ShowSeatStatus;
import com.moviebooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Belt-and-suspenders cleanup for time-bound seat holds. Reads (via
 * {@link com.moviebooking.entity.ShowSeat#isHoldExpired}) already treat an expired
 * HELD row as available, so a stuck hold never actually blocks another customer --
 * but this sweep periodically flips the DB row itself back to AVAILABLE so the
 * inventory is clean at rest, admin views are accurate, and there's no unbounded
 * buildup of stale HELD rows.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldExpiryScheduler {

    private final ShowSeatRepository showSeatRepository;

    @Scheduled(fixedRateString = "${app.booking.hold-sweep-interval-ms}")
    @Transactional
    public void releaseExpiredHolds() {
        Instant now = Instant.now();
        List<ShowSeat> expired = showSeatRepository.findExpiredHolds(now);
        if (expired.isEmpty()) {
            return;
        }
        // Re-lock each expired row before flipping it, in case a booking request is
        // concurrently converting the very same hold into a confirmed booking.
        List<Long> ids = expired.stream().map(ShowSeat::getId).sorted().toList();
        List<ShowSeat> locked = showSeatRepository.lockForUpdate(ids);
        int released = 0;
        for (ShowSeat seat : locked) {
            if (seat.isHoldExpired(now)) {
                seat.setStatus(ShowSeatStatus.AVAILABLE);
                seat.setHeldByUser(null);
                seat.setHoldExpiresAt(null);
                released++;
            }
        }
        showSeatRepository.saveAll(locked);
        if (released > 0) {
            log.info("Swept {} expired seat hold(s) back to AVAILABLE", released);
        }
    }
}
