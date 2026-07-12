package com.moviebooking.repository;

import com.moviebooking.entity.ShowSeat;
import com.moviebooking.entity.ShowSeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    /**
     * Takes a row-level PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE) on the given
     * seats. Any other transaction trying to lock the same rows -- e.g. two users
     * racing to hold the same seat -- blocks until this transaction commits or rolls
     * back, which is what gives us "no double allocation" without a distributed lock.
     * Always call this inside a single @Transactional method with a bounded scope so
     * the row lock is held for as short a time as possible.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.id in :ids")
    List<ShowSeat> lockForUpdate(@Param("ids") List<Long> ids);

    @Query("""
        select s from ShowSeat s
        where s.status = com.moviebooking.entity.ShowSeatStatus.HELD
          and s.holdExpiresAt < :now
        """)
    List<ShowSeat> findExpiredHolds(@Param("now") Instant now);

    long countByShowIdAndStatus(Long showId, ShowSeatStatus status);
}
