package com.moviebooking.controller;

import com.moviebooking.dto.request.CityRequest;
import com.moviebooking.dto.request.ScreenRequest;
import com.moviebooking.dto.request.TheaterRequest;
import com.moviebooking.dto.response.CityResponse;
import com.moviebooking.dto.response.ScreenResponse;
import com.moviebooking.dto.response.TheaterResponse;
import com.moviebooking.service.CityService;
import com.moviebooking.service.ScreenService;
import com.moviebooking.service.TheaterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Admin-only venue management: cities, theaters, screens/seat layouts. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminVenueController {

    private final CityService cityService;
    private final TheaterService theaterService;
    private final ScreenService screenService;

    @PostMapping("/cities")
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CityResponse.from(cityService.create(request)));
    }

    @DeleteMapping("/cities/{id}")
    public ResponseEntity<Void> deactivateCity(@PathVariable Long id) {
        cityService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/theaters")
    public ResponseEntity<TheaterResponse> createTheater(@Valid @RequestBody TheaterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TheaterResponse.from(theaterService.create(request)));
    }

    @PostMapping("/screens")
    public ResponseEntity<ScreenResponse> createScreen(@Valid @RequestBody ScreenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ScreenResponse.from(screenService.create(request)));
    }

    @GetMapping("/screens/{id}")
    public ScreenResponse getScreen(@PathVariable Long id) {
        return ScreenResponse.from(screenService.get(id));
    }
}
