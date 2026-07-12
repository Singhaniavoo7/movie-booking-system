package com.moviebooking.service;

import com.moviebooking.entity.Booking;
import com.moviebooking.entity.Payment;
import com.moviebooking.entity.PaymentStatus;
import com.moviebooking.exception.PaymentFailedException;
import com.moviebooking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mock payment gateway. There is no real payment provider in scope for this
 * exercise; this simulates the charge/refund contract a real integration
 * (Stripe/Razorpay/etc.) would expose, including a deterministic failure hook so
 * the "payment declined -> seats released" path is exercisable in tests without
 * relying on randomness.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment charge(Booking booking, String method, boolean simulateFailure) {
        if (simulateFailure) {
            paymentRepository.save(Payment.builder()
                    .booking(booking).amount(booking.getFinalAmount()).method(method)
                    .transactionRef("TXN-" + UUID.randomUUID())
                    .status(PaymentStatus.FAILED)
                    .build());
            throw new PaymentFailedException("Payment was declined by the gateway");
        }
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getFinalAmount())
                .method(method)
                .transactionRef("TXN-" + UUID.randomUUID())
                .status(PaymentStatus.SUCCESS)
                .build();
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refund(Booking booking, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new IllegalStateException("No payment on record for booking " + booking.getId()));

        payment.setRefundedAmount(refundAmount);
        payment.setRefundedAt(Instant.now());
        payment.setStatus(refundAmount.compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED);
        return paymentRepository.save(payment);
    }
}
