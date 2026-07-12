package com.moviebooking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ScreenRequest {
    @NotBlank
    private String name;

    @NotNull
    private Long theaterId;

    @NotEmpty
    @Valid
    private List<SeatRowRequest> seatLayout;
}
