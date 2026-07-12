package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SeatHoldResponse {
    private List<Long> showSeatIds;
    private Instant holdExpiresAt;
}
