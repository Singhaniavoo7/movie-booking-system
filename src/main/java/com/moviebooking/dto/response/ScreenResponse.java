package com.moviebooking.dto.response;

import com.moviebooking.entity.Screen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ScreenResponse {
    private Long id;
    private String name;
    private Long theaterId;
    private int totalSeats;
    private List<SeatSummary> seats;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SeatSummary {
        private Long id;
        private String rowLabel;
        private int seatNumber;
        private String seatType;
    }

    public static ScreenResponse from(Screen s) {
        List<SeatSummary> seats = s.getSeatTemplates().stream()
                .map(t -> SeatSummary.builder()
                        .id(t.getId()).rowLabel(t.getRowLabel())
                        .seatNumber(t.getSeatNumber()).seatType(t.getSeatType().name())
                        .build())
                .toList();
        return ScreenResponse.builder()
                .id(s.getId()).name(s.getName()).theaterId(s.getTheater().getId())
                .totalSeats(seats.size()).seats(seats)
                .build();
    }
}
