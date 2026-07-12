package com.moviebooking.repository;

import com.moviebooking.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("""
        select s from Show s
        join fetch s.movie m
        join fetch s.screen sc
        join fetch sc.theater t
        join fetch t.city c
        where (:cityId is null or c.id = :cityId)
          and (:movieId is null or m.id = :movieId)
          and s.startTime >= :from
          and s.startTime < :to
          and s.cancelled = false
        order by s.startTime asc
        """)
    List<Show> search(@Param("cityId") Long cityId,
                       @Param("movieId") Long movieId,
                       @Param("from") Instant from,
                       @Param("to") Instant to);
}
