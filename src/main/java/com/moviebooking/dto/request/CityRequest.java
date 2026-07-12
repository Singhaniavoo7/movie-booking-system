package com.moviebooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CityRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String state;
}
