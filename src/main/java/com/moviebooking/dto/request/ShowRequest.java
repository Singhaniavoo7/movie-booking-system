package com.moviebooking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ShowRequest {
    @NotNull
    private Long movieId;

    @NotNull
    private Long screenId;

    @NotNull
    private Long pricingTierId;

    @NotNull
    private Instant startTime;
}
