package com.moviebooking.dto.request;

import com.moviebooking.entity.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class PricingTierRequest {
    @NotBlank
    private String name;

    @NotEmpty
    private Map<SeatType, BigDecimal> seatTypePrices;
}
