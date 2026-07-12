package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * A named pricing tier (e.g. "WEEKDAY", "WEEKEND", "HOLIDAY") holding a base price
 * per {@link SeatType}. A {@link Show} is assigned exactly one tier at creation time,
 * which -- together with each seat's type -- determines the price snapshot stored on
 * every {@link ShowSeat} when the show is created.
 */
@Entity
@Table(name = "pricing_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingTier extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    /** Base price per seat type, e.g. REGULAR -> 200.00, PREMIUM -> 350.00. */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pricing_tier_rates", joinColumns = @JoinColumn(name = "pricing_tier_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "seat_type")
    @Column(name = "price", nullable = false)
    private Map<SeatType, BigDecimal> seatTypePrices = new HashMap<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
