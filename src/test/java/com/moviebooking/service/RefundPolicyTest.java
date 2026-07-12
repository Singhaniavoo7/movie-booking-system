package com.moviebooking.service;

import com.moviebooking.entity.RefundPolicy;
import com.moviebooking.entity.RefundRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RefundPolicyTest {

    private RefundPolicy policy;

    @BeforeEach
    void setUp() {
        // 100% refund if cancelled >= 24h before show, 50% if >= 2h, else 0%.
        policy = RefundPolicy.builder()
                .name("Standard")
                .rules(List.of(
                        RefundRule.builder().minHoursBeforeShow(24).refundPercentage(BigDecimal.valueOf(100)).build(),
                        RefundRule.builder().minHoursBeforeShow(2).refundPercentage(BigDecimal.valueOf(50)).build(),
                        RefundRule.builder().minHoursBeforeShow(0).refundPercentage(BigDecimal.ZERO).build()
                ))
                .build();
    }

    @Test
    void fullRefund_whenCancelledWellBeforeShow() {
        assertThat(policy.resolveRefundPercentage(48)).isEqualByComparingTo("100");
    }

    @Test
    void fullRefund_atExactTwentyFourHourBoundary() {
        assertThat(policy.resolveRefundPercentage(24)).isEqualByComparingTo("100");
    }

    @Test
    void partialRefund_betweenTwoAndTwentyFourHours() {
        assertThat(policy.resolveRefundPercentage(10)).isEqualByComparingTo("50");
    }

    @Test
    void noRefund_lessThanTwoHoursBeforeShow() {
        assertThat(policy.resolveRefundPercentage(1)).isEqualByComparingTo("0");
    }

    @Test
    void noRefund_afterShowHasAlreadyStarted() {
        // Negative "hours before show" means the show already started.
        assertThat(policy.resolveRefundPercentage(-5)).isEqualByComparingTo("0");
    }

    @Test
    void zeroRefund_whenPolicyHasNoRules() {
        RefundPolicy empty = RefundPolicy.builder().name("None").rules(List.of()).build();
        assertThat(empty.resolveRefundPercentage(100)).isEqualByComparingTo("0");
    }
}
