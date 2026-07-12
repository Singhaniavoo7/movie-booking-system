package com.moviebooking.controller;

import com.moviebooking.dto.response.CityResponse;
import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.dto.response.TheaterResponse;
import com.moviebooking.service.CityService;
import com.moviebooking.service.MovieService;
import com.moviebooking.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Public, unauthenticated read endpoints for browsing the catalog. */
@RestController
@RequiredArgsConstructor
public class CatalogController {

    private final CityService cityService;
    private final TheaterService theaterService;
    private final MovieService movieService;

    @GetMapping("/api/cities")
    public List<CityResponse> listCities() {
        return cityService.listActive().stream().map(CityResponse::from).toList();
    }

    @GetMapping("/api/theaters")
    public List<TheaterResponse> listTheaters(@RequestParam Long cityId) {
        return theaterService.listByCity(cityId).stream().map(TheaterResponse::from).toList();
    }

    @GetMapping("/api/movies")
    public List<MovieResponse> listMovies() {
        return movieService.listActive().stream().map(MovieResponse::from).toList();
    }
}
