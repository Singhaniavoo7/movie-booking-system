package com.moviebooking.controller;

import com.moviebooking.dto.request.SeatHoldRequest;
import com.moviebooking.dto.response.SeatHoldResponse;
import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.dto.response.ShowSeatResponse;
import com.moviebooking.entity.ShowSeat;
import com.moviebooking.entity.User;
import com.moviebooking.repository.ShowSeatRepository;
import com.moviebooking.security.AppUserPrincipal;
import com.moviebooking.security.CurrentUserResolver;
import com.moviebooking.service.SeatHoldService;
import com.moviebooking.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;
    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldService seatHoldService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public List<ShowResponse> search(@RequestParam(required = false) Long cityId,
                                      @RequestParam(required = false) Long movieId,
                                      @RequestParam(required = false) Instant from,
                                      @RequestParam(required = false) Instant to) {
        return showService.search(cityId, movieId, from, to).stream().map(ShowResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ShowResponse get(@PathVariable Long id) {
        return ShowResponse.from(showService.get(id));
    }

    @GetMapping("/{id}/seats")
    public List<ShowSeatResponse> seatMap(@PathVariable Long id) {
        showService.get(id); // 404s if the show doesn't exist
        Instant now = Instant.now();
        List<ShowSeat> seats = showSeatRepository.findByShowId(id);
        return seats.stream().map(s -> ShowSeatResponse.from(s, now)).toList();
    }

    @PostMapping("/{id}/holds")
    public ResponseEntity<SeatHoldResponse> holdSeats(@PathVariable Long id,
                                                        @Valid @RequestBody SeatHoldRequest request,
                                                        @AuthenticationPrincipal AppUserPrincipal principal) {
        User user = currentUserResolver.resolve(principal);
        List<ShowSeat> held = seatHoldService.holdSeats(id, request.getShowSeatIds(), user);
        Instant expiresAt = held.get(0).getHoldExpiresAt();
        SeatHoldResponse response = SeatHoldResponse.builder()
                .showSeatIds(held.stream().map(ShowSeat::getId).toList())
                .holdExpiresAt(expiresAt)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/holds")
    public ResponseEntity<Void> releaseHold(@PathVariable Long id,
                                             @Valid @RequestBody SeatHoldRequest request,
                                             @AuthenticationPrincipal AppUserPrincipal principal) {
        User user = currentUserResolver.resolve(principal);
        seatHoldService.releaseSeats(request.getShowSeatIds(), user);
        return ResponseEntity.noContent().build();
    }
}
