package com.moviebooking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SeatHoldRequest {
    @NotEmpty
    private List<Long> showSeatIds;
}
