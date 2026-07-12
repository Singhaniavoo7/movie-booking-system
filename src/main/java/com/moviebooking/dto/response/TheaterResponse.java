package com.moviebooking.dto.response;

import com.moviebooking.entity.Theater;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TheaterResponse {
    private Long id;
    private String name;
    private String address;
    private Long cityId;
    private String cityName;

    public static TheaterResponse from(Theater t) {
        return TheaterResponse.builder()
                .id(t.getId()).name(t.getName()).address(t.getAddress())
                .cityId(t.getCity().getId()).cityName(t.getCity().getName())
                .build();
    }
}
