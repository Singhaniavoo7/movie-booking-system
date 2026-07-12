package com.moviebooking.service;

import com.moviebooking.dto.request.TheaterRequest;
import com.moviebooking.entity.City;
import com.moviebooking.entity.Theater;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final CityService cityService;

    @Transactional
    public Theater create(TheaterRequest req) {
        City city = cityService.get(req.getCityId());
        Theater theater = Theater.builder()
                .name(req.getName()).address(req.getAddress()).city(city).active(true)
                .build();
        return theaterRepository.save(theater);
    }

    public List<Theater> listByCity(Long cityId) {
        return theaterRepository.findByCityIdAndActiveTrue(cityId);
    }

    public Theater get(Long id) {
        return theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found: " + id));
    }
}
