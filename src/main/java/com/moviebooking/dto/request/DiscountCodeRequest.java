package com.moviebooking.dto.request;

import com.moviebooking.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class DiscountCodeRequest {
    @NotBlank
    private String code;

    @NotNull
    private DiscountType type;

    @NotNull
    private BigDecimal value;

    private BigDecimal minBookingAmount;

    private BigDecimal maxDiscountAmount;

    @NotNull
    private Instant validFrom;

    @NotNull
    private Instant validTo;

    private Integer maxUses;
}
