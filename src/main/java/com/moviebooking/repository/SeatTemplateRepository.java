package com.moviebooking.repository;

import com.moviebooking.entity.SeatTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, Long> {
    List<SeatTemplate> findByScreenId(Long screenId);
}
