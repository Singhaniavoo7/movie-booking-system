package com.moviebooking.repository;

import com.moviebooking.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByActiveTrue();
}
