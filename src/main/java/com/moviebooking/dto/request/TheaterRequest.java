package com.moviebooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TheaterRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @NotNull
    private Long cityId;
}
