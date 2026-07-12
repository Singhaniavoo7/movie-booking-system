package com.moviebooking.service;

import com.moviebooking.dto.request.CreateBookingRequest;
import com.moviebooking.entity.*;
import com.moviebooking.event.BookingCancelledEvent;
import com.moviebooking.event.BookingConfirmedEvent;
import com.moviebooking.exception.ForbiddenActionException;
import com.moviebooking.exception.IllegalBookingStateException;
import com.moviebooking.exception.InvalidDiscountException;
import com.moviebooking.exception.PaymentFailedException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.BookingRepository;
import com.moviebooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowService showService;
    private final SeatHoldService seatHoldService;
    private final DiscountCodeService discountCodeService;
    private final RefundPolicyService refundPolicyService;
    private final PaymentService paymentService;
    private final PricingCalculator pricingCalculator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Confirms a booking from a set of held seats. Re-locks the exact ShowSeat rows
     * (same lock-ordering discipline as SeatHoldService) so that hold-validation and
     * the BOOKED status flip happen atomically -- no other request can steal or
     * expire-sweep these seats between the check and the write.
     */
    @Transactional(noRollbackFor = PaymentFailedException.class)
    public Booking createBooking(CreateBookingRequest request, User user) {
        Show show = showService.get(request.getShowId());

        List<Long> sortedIds = request.getShowSeatIds().stream().sorted().distinct().toList();
        List<ShowSeat> seats = showSeatRepository.lockForUpdate(sortedIds);
        if (seats.size() != sortedIds.size()) {
            throw new ResourceNotFoundException("One or more seats were not found");
        }

        Instant now = Instant.now();
        seatHoldService.assertHeldByUser(seats, user, now);

        BigDecimal subtotal = seats.stream().map(ShowSeat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        DiscountCode discountCode = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            discountCode = discountCodeService.findByCode(request.getDiscountCode())
                    .orElseThrow(() -> new InvalidDiscountException("Discount code not found: " + request.getDiscountCode()));
            pricingCalculator.validateDiscount(discountCode, subtotal, now);
            discountAmount = pricingCalculator.computeDiscount(discountCode, subtotal);
        }
        BigDecimal finalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        RefundPolicy refundPolicy = refundPolicyService.getDefault().orElse(null);

        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .discountCode(discountCode)
                .refundPolicy(refundPolicy)
                .subtotalAmount(subtotal)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(BookingStatus.PENDING_PAYMENT)
                .bookingReference(generateReference())
                .build();

        List<BookingSeat> bookingSeats = seats.stream()
                .map(seat -> BookingSeat.builder().booking(booking).showSeat(seat).priceAtBooking(seat.getPrice()).build())
                .toList();
        booking.setBookingSeats(bookingSeats);
        booking = bookingRepository.save(booking);

        try {
            paymentService.charge(booking, request.getPaymentMethod(), request.isSimulatePaymentFailure());
        } catch (PaymentFailedException ex) {
            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            releaseSeatsBackToAvailable(seats);
            bookingRepository.save(booking);
            log.warn("Payment failed for booking {} (user {}); seats released", booking.getBookingReference(), user.getEmail());
            throw ex;
        }

        for (ShowSeat seat : seats) {
            seat.setStatus(ShowSeatStatus.BOOKED);
            seat.setHoldExpiresAt(null);
        }
        showSeatRepository.saveAll(seats);

        if (discountCode != null) {
            discountCode.setUsedCount(discountCode.getUsedCount() + 1);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingConfirmedEvent(booking.getId()));
        return booking;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, User user) {
        Booking booking = get(bookingId);
        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenActionException("You can only cancel your own bookings");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalBookingStateException("Only confirmed bookings can be cancelled (current status: " + booking.getStatus() + ")");
        }

        long hoursBeforeShow = Duration.between(Instant.now(), booking.getShow().getStartTime()).toHours();
        BigDecimal refundPercentage = booking.getRefundPolicy() != null
                ? booking.getRefundPolicy().resolveRefundPercentage(hoursBeforeShow)
                : BigDecimal.ZERO;
        BigDecimal refundAmount = pricingCalculator.computeRefundAmount(booking.getFinalAmount(), refundPercentage);

        paymentService.refund(booking, refundAmount);

        List<Long> showSeatIds = booking.getBookingSeats().stream().map(bs -> bs.getShowSeat().getId()).sorted().toList();
        List<ShowSeat> seats = showSeatRepository.lockForUpdate(showSeatIds);
        releaseSeatsBackToAvailable(seats);
        showSeatRepository.saveAll(seats);

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingCancelledEvent(booking.getId()));
        return booking;
    }

    public List<Booking> getHistory(User user) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Booking get(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

    private void releaseSeatsBackToAvailable(List<ShowSeat> seats) {
        for (ShowSeat seat : seats) {
            seat.setStatus(ShowSeatStatus.AVAILABLE);
            seat.setHeldByUser(null);
            seat.setHoldExpiresAt(null);
        }
    }

    private String generateReference() {
        return "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
