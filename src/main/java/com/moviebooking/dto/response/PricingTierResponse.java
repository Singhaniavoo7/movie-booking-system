package com.moviebooking.dto.response;

import com.moviebooking.entity.PricingTier;
import com.moviebooking.entity.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class PricingTierResponse {
    private Long id;
    private String name;
    private Map<SeatType, BigDecimal> seatTypePrices;

    public static PricingTierResponse from(PricingTier p) {
        return PricingTierResponse.builder().id(p.getId()).name(p.getName()).seatTypePrices(p.getSeatTypePrices()).build();
    }
}
