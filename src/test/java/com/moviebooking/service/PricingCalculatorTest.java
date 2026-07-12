package com.moviebooking.service;

import com.moviebooking.entity.DiscountCode;
import com.moviebooking.entity.DiscountType;
import com.moviebooking.exception.InvalidDiscountException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    private DiscountCode.DiscountCodeBuilder validCode() {
        Instant now = Instant.now();
        return DiscountCode.builder()
                .code("SAVE10")
                .validFrom(now.minus(1, ChronoUnit.DAYS))
                .validTo(now.plus(1, ChronoUnit.DAYS))
                .active(true)
                .usedCount(0);
    }

    @Test
    void percentageDiscount_isAppliedCorrectly() {
        DiscountCode code = validCode().type(DiscountType.PERCENTAGE).value(BigDecimal.valueOf(10)).build();
        BigDecimal discount = calculator.computeDiscount(code, BigDecimal.valueOf(1000));
        assertThat(discount).isEqualByComparingTo("100.00");
    }

    @Test
    void percentageDiscount_isCappedByMaxDiscountAmount() {
        DiscountCode code = validCode().type(DiscountType.PERCENTAGE).value(BigDecimal.valueOf(50))
                .maxDiscountAmount(BigDecimal.valueOf(75)).build();
        BigDecimal discount = calculator.computeDiscount(code, BigDecimal.valueOf(1000));
        assertThat(discount).isEqualByComparingTo("75");
    }

    @Test
    void flatDiscount_neverExceedsSubtotal() {
        DiscountCode code = validCode().type(DiscountType.FLAT).value(BigDecimal.valueOf(500)).build();
        BigDecimal discount = calculator.computeDiscount(code, BigDecimal.valueOf(200));
        assertThat(discount).isEqualByComparingTo("200"); // can't discount more than the booking costs
    }

    @Test
    void validateDiscount_rejectsExpiredCode() {
        DiscountCode expired = validCode()
                .type(DiscountType.FLAT).value(BigDecimal.TEN)
                .validTo(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        assertThatThrownBy(() -> calculator.validateDiscount(expired, BigDecimal.valueOf(100), Instant.now()))
                .isInstanceOf(InvalidDiscountException.class);
    }

    @Test
    void validateDiscount_rejectsWhenBelowMinimumBookingAmount() {
        DiscountCode code = validCode().type(DiscountType.FLAT).value(BigDecimal.TEN)
                .minBookingAmount(BigDecimal.valueOf(500)).build();
        assertThatThrownBy(() -> calculator.validateDiscount(code, BigDecimal.valueOf(100), Instant.now()))
                .isInstanceOf(InvalidDiscountException.class);
    }

    @Test
    void validateDiscount_rejectsWhenMaxUsesExhausted() {
        DiscountCode code = validCode().type(DiscountType.FLAT).value(BigDecimal.TEN)
                .maxUses(5).usedCount(5).build();
        assertThatThrownBy(() -> calculator.validateDiscount(code, BigDecimal.valueOf(100), Instant.now()))
                .isInstanceOf(InvalidDiscountException.class);
    }

    @Test
    void refundAmount_isPercentageOfAmountPaid() {
        BigDecimal refund = calculator.computeRefundAmount(BigDecimal.valueOf(200), BigDecimal.valueOf(50));
        assertThat(refund).isEqualByComparingTo("100.00");
    }
}
