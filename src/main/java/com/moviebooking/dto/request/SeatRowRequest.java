package com.moviebooking.dto.request;

import com.moviebooking.entity.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** One row of a screen's seat layout, e.g. row "A", 12 REGULAR seats. */
@Getter
@Setter
public class SeatRowRequest {
    @NotBlank
    private String rowLabel;

    @Min(1)
    private int seatCount;

    @NotNull
    private SeatType seatType;
}
