package com.moviebooking.controller;

import com.moviebooking.dto.request.MovieRequest;
import com.moviebooking.dto.request.PricingTierRequest;
import com.moviebooking.dto.request.ShowRequest;
import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.dto.response.PricingTierResponse;
import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.service.MovieService;
import com.moviebooking.service.PricingTierService;
import com.moviebooking.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin-only catalog management: movies, pricing tiers, and shows. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final MovieService movieService;
    private final PricingTierService pricingTierService;
    private final ShowService showService;

    @PostMapping("/movies")
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(MovieResponse.from(movieService.create(request)));
    }

    @PostMapping("/pricing-tiers")
    public ResponseEntity<PricingTierResponse> createPricingTier(@Valid @RequestBody PricingTierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PricingTierResponse.from(pricingTierService.create(request)));
    }

    @GetMapping("/pricing-tiers")
    public List<PricingTierResponse> listPricingTiers() {
        return pricingTierService.listAll().stream().map(PricingTierResponse::from).toList();
    }

    @PostMapping("/shows")
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody ShowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ShowResponse.from(showService.create(request)));
    }
}
