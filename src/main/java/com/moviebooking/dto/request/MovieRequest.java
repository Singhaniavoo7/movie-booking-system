package com.moviebooking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieRequest {
    @NotBlank
    private String title;

    private String language;

    private String genre;

    @Min(1)
    private int durationMinutes;

    private String description;
}
