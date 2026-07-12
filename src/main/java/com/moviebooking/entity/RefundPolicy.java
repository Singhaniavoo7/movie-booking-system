package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "refund_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPolicy extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "refund_policy_rules", joinColumns = @JoinColumn(name = "refund_policy_id"))
    private List<RefundRule> rules = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Resolves the refund percentage (0-100) for a cancellation happening
     * {@code hoursBeforeShow} hours ahead of the show's start time. Picks the rule
     * with the highest {@code minHoursBeforeShow} threshold that is still satisfied.
     * Returns 0 if no rule matches (e.g. cancelling after the show has started).
     */
    @Transient
    public BigDecimal resolveRefundPercentage(long hoursBeforeShow) {
        return rules.stream()
                .filter(r -> hoursBeforeShow >= r.getMinHoursBeforeShow())
                .max(Comparator.comparingInt(RefundRule::getMinHoursBeforeShow))
                .map(RefundRule::getRefundPercentage)
                .orElse(BigDecimal.ZERO);
    }
}
