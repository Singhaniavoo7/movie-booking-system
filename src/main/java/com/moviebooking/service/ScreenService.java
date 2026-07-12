package com.moviebooking.service;

import com.moviebooking.dto.request.ScreenRequest;
import com.moviebooking.dto.request.SeatRowRequest;
import com.moviebooking.entity.Screen;
import com.moviebooking.entity.SeatTemplate;
import com.moviebooking.entity.Theater;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.ScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterService theaterService;

    @Transactional
    public Screen create(ScreenRequest req) {
        Theater theater = theaterService.get(req.getTheaterId());

        Screen screen = Screen.builder().name(req.getName()).theater(theater).build();

        List<SeatTemplate> seats = new ArrayList<>();
        for (SeatRowRequest row : req.getSeatLayout()) {
            for (int seatNo = 1; seatNo <= row.getSeatCount(); seatNo++) {
                seats.add(SeatTemplate.builder()
                        .screen(screen)
                        .rowLabel(row.getRowLabel())
                        .seatNumber(seatNo)
                        .seatType(row.getSeatType())
                        .build());
            }
        }
        screen.setSeatTemplates(seats);

        return screenRepository.save(screen);
    }

    public List<Screen> listByTheater(Long theaterId) {
        return screenRepository.findByTheaterId(theaterId);
    }

    public Screen get(Long id) {
        return screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + id));
    }
}
