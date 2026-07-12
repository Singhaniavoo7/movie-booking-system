package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "discount_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCode extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType type;

    /** Percentage (0-100) if type == PERCENTAGE, absolute currency amount if type == FLAT. */
    @Column(nullable = false)
    private BigDecimal value;

    private BigDecimal minBookingAmount;

    private BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    private Instant validFrom;

    @Column(nullable = false)
    private Instant validTo;

    /** Null = unlimited uses. */
    private Integer maxUses;

    @Builder.Default
    @Column(nullable = false)
    private int usedCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Transient
    public boolean isValidAt(Instant now) {
        if (!active) return false;
        if (now.isBefore(validFrom) || now.isAfter(validTo)) return false;
        return maxUses == null || usedCount < maxUses;
    }
}
