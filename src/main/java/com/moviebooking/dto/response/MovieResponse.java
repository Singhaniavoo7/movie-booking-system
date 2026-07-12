package com.moviebooking.dto.response;

import com.moviebooking.entity.Movie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MovieResponse {
    private Long id;
    private String title;
    private String language;
    private String genre;
    private int durationMinutes;
    private String description;

    public static MovieResponse from(Movie m) {
        return MovieResponse.builder()
                .id(m.getId()).title(m.getTitle()).language(m.getLanguage())
                .genre(m.getGenre()).durationMinutes(m.getDurationMinutes())
                .description(m.getDescription())
                .build();
    }
}
