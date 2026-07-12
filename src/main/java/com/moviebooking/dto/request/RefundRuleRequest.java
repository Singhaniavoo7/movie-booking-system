package com.moviebooking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RefundRuleRequest {
    @Min(0)
    private int minHoursBeforeShow;

    @NotNull
    private BigDecimal refundPercentage;
}
