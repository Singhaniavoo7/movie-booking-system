package com.moviebooking.service;

import com.moviebooking.dto.request.CityRequest;
import com.moviebooking.entity.City;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Transactional
    public City create(CityRequest req) {
        City city = City.builder().name(req.getName()).state(req.getState()).active(true).build();
        return cityRepository.save(city);
    }

    public List<City> listActive() {
        return cityRepository.findByActiveTrue();
    }

    public City get(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + id));
    }

    @Transactional
    public void deactivate(Long id) {
        City city = get(id);
        city.setActive(false);
    }
}
