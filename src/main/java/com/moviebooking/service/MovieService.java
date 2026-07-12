package com.moviebooking.service;

import com.moviebooking.dto.request.MovieRequest;
import com.moviebooking.entity.Movie;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    @Transactional
    public Movie create(MovieRequest req) {
        Movie movie = Movie.builder()
                .title(req.getTitle()).language(req.getLanguage()).genre(req.getGenre())
                .durationMinutes(req.getDurationMinutes()).description(req.getDescription())
                .active(true)
                .build();
        return movieRepository.save(movie);
    }

    public List<Movie> listActive() {
        return movieRepository.findByActiveTrue();
    }

    public Movie get(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + id));
    }
}
