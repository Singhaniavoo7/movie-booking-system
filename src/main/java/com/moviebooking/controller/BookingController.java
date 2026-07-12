package com.moviebooking.controller;

import com.moviebooking.dto.request.CreateBookingRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.dto.response.CancelBookingResponse;
import com.moviebooking.entity.Booking;
import com.moviebooking.entity.Role;
import com.moviebooking.entity.User;
import com.moviebooking.exception.ForbiddenActionException;
import com.moviebooking.security.AppUserPrincipal;
import com.moviebooking.security.CurrentUserResolver;
import com.moviebooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request,
                                                    @AuthenticationPrincipal AppUserPrincipal principal) {
        User user = currentUserResolver.resolve(principal);
        Booking booking = bookingService.createBooking(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking));
    }

    @GetMapping
    public List<BookingResponse> history(@AuthenticationPrincipal AppUserPrincipal principal) {
        User user = currentUserResolver.resolve(principal);
        return bookingService.getHistory(user).stream().map(BookingResponse::from).toList();
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        User user = currentUserResolver.resolve(principal);
        Booking booking = bookingService.get(id);
        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenActionException("You can only view your own bookings");
        }
        return BookingResponse.from(booking);
    }

    @PostMapping("/{id}/cancel")
    public CancelBookingResponse cancel(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        User user = currentUserResolver.resolve(principal);
        BigDecimal amountPaid;
        Booking before = bookingService.get(id);
        amountPaid = before.getFinalAmount();

        Booking cancelled = bookingService.cancelBooking(id, user);

        // Refund % actually applied is derivable from policy at cancel time; recomputed
        // here purely for the response summary (cancelBooking already persisted the refund).
        long hoursBeforeShow = java.time.Duration.between(java.time.Instant.now(), cancelled.getShow().getStartTime()).toHours();
        BigDecimal refundPct = cancelled.getRefundPolicy() != null
                ? cancelled.getRefundPolicy().resolveRefundPercentage(hoursBeforeShow)
                : BigDecimal.ZERO;
        BigDecimal refundAmount = amountPaid.multiply(refundPct)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        return CancelBookingResponse.builder()
                .bookingId(cancelled.getId())
                .status(cancelled.getStatus().name())
                .amountPaid(amountPaid)
                .refundPercentageApplied(refundPct)
                .refundAmount(refundAmount)
                .build();
    }
}
