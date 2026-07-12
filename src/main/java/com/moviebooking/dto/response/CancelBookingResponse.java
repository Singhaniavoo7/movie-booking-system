package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class CancelBookingResponse {
    private Long bookingId;
    private String status;
    private BigDecimal amountPaid;
    private BigDecimal refundPercentageApplied;
    private BigDecimal refundAmount;
}
