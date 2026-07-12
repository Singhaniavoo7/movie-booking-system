package com.moviebooking.dto.response;

import com.moviebooking.entity.Show;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class ShowResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Long theaterId;
    private String theaterName;
    private Long screenId;
    private String screenName;
    private String cityName;
    private Instant startTime;
    private Instant endTime;
    private String pricingTierName;

    public static ShowResponse from(Show s) {
        return ShowResponse.builder()
                .id(s.getId())
                .movieId(s.getMovie().getId())
                .movieTitle(s.getMovie().getTitle())
                .theaterId(s.getScreen().getTheater().getId())
                .theaterName(s.getScreen().getTheater().getName())
                .screenId(s.getScreen().getId())
                .screenName(s.getScreen().getName())
                .cityName(s.getScreen().getTheater().getCity().getName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .pricingTierName(s.getPricingTier().getName())
                .build();
    }
}
