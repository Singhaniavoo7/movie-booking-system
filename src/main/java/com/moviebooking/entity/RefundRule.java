package com.moviebooking.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

/**
 * One tier of a {@link RefundPolicy}: if the cancellation happens at least
 * {@code minHoursBeforeShow} before the show starts, {@code refundPercentage} of the
 * paid amount is refunded. A policy holds several rules; the matching rule with the
 * largest {@code minHoursBeforeShow} that the cancellation still satisfies wins.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRule {

    private int minHoursBeforeShow;

    /** 0-100 */
    private BigDecimal refundPercentage;
}
