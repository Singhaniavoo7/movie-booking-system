package com.moviebooking.service;

import com.moviebooking.entity.DiscountCode;
import com.moviebooking.exception.InvalidDiscountException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Stateless pricing math, deliberately kept free of persistence/transaction
 * concerns so it's trivial to unit test in isolation from Spring/JPA.
 */
@Component
public class PricingCalculator {

    public void validateDiscount(DiscountCode discountCode, BigDecimal subtotal, Instant now) {
        if (!discountCode.isValidAt(now)) {
            throw new InvalidDiscountException("Discount code is expired, inactive, or exhausted: " + discountCode.getCode());
        }
        if (discountCode.getMinBookingAmount() != null && subtotal.compareTo(discountCode.getMinBookingAmount()) < 0) {
            throw new InvalidDiscountException(
                    "This code requires a minimum booking amount of " + discountCode.getMinBookingAmount());
        }
    }

    public BigDecimal computeDiscount(DiscountCode discountCode, BigDecimal subtotal) {
        BigDecimal discount = switch (discountCode.getType()) {
            case PERCENTAGE -> subtotal.multiply(discountCode.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FLAT -> discountCode.getValue();
        };
        if (discountCode.getMaxDiscountAmount() != null) {
            discount = discount.min(discountCode.getMaxDiscountAmount());
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    public BigDecimal computeRefundAmount(BigDecimal amountPaid, BigDecimal refundPercentage) {
        return amountPaid.multiply(refundPercentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
