package com.moviebooking.service;

import com.moviebooking.entity.Booking;
import com.moviebooking.event.BookingCancelledEvent;
import com.moviebooking.event.BookingConfirmedEvent;
import com.moviebooking.event.ShowReminderEvent;
import com.moviebooking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Service;

/**
 * Sends booking confirmation / cancellation / reminder notifications.
 *
 * Listens with {@link TransactionalEventListener}(AFTER_COMMIT) so a notification is
 * only ever fired once the booking transaction has actually committed (never on a
 * rollback), and {@link Async} so the send itself runs on a background thread and
 * never blocks or slows down the HTTP response to the customer. In this take-home,
 * "sending" is simulated with structured logging; swapping in a real email/SMS/push
 * provider only touches this class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final BookingRepository bookingRepository;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        bookingRepository.findById(event.bookingId()).ifPresent(this::sendConfirmation);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        bookingRepository.findById(event.bookingId()).ifPresent(this::sendCancellation);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShowReminder(ShowReminderEvent event) {
        bookingRepository.findById(event.bookingId()).ifPresent(this::sendReminder);
    }

    private void sendConfirmation(Booking booking) {
        log.info("[NOTIFY] Booking CONFIRMED -> user={} ref={} show='{}' amount={}",
                booking.getUser().getEmail(), booking.getBookingReference(),
                booking.getShow().getMovie().getTitle(), booking.getFinalAmount());
    }

    private void sendCancellation(Booking booking) {
        log.info("[NOTIFY] Booking CANCELLED -> user={} ref={}",
                booking.getUser().getEmail(), booking.getBookingReference());
    }

    private void sendReminder(Booking booking) {
        log.info("[NOTIFY] Show REMINDER -> user={} ref={} show='{}' startsAt={}",
                booking.getUser().getEmail(), booking.getBookingReference(),
                booking.getShow().getMovie().getTitle(), booking.getShow().getStartTime());
    }
}
