package com.moviebooking.dto.response;

import com.moviebooking.entity.City;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CityResponse {
    private Long id;
    private String name;
    private String state;

    public static CityResponse from(City c) {
        return CityResponse.builder().id(c.getId()).name(c.getName()).state(c.getState()).build();
    }
}
