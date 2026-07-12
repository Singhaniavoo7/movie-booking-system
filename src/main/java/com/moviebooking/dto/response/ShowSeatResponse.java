package com.moviebooking.dto.response;

import com.moviebooking.entity.ShowSeat;
import com.moviebooking.entity.ShowSeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class ShowSeatResponse {
    private Long showSeatId;
    private String rowLabel;
    private int seatNumber;
    private String seatType;
    private BigDecimal price;
    private ShowSeatStatus status;

    public static ShowSeatResponse from(ShowSeat s, java.time.Instant now) {
        // Present lazily-expired holds as AVAILABLE to the client even before the
        // sweep job has run.
        ShowSeatStatus effectiveStatus = s.isHoldExpired(now) ? ShowSeatStatus.AVAILABLE : s.getStatus();
        return ShowSeatResponse.builder()
                .showSeatId(s.getId())
                .rowLabel(s.getSeatTemplate().getRowLabel())
                .seatNumber(s.getSeatTemplate().getSeatNumber())
                .seatType(s.getSeatTemplate().getSeatType().name())
                .price(s.getPrice())
                .status(effectiveStatus)
                .build();
    }
}
