package com.moviebooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Common auditing + optimistic-locking fields shared by all persistent entities.
 * The {@code version} column backs optimistic concurrency control (JPA/Hibernate
 * bumps it on every UPDATE and throws {@link jakarta.persistence.OptimisticLockException}
 * on a stale write) which we layer on top of explicit pessimistic locks for the
 * seat-hold hot path -- see {@code ShowSeatRepository#lockForUpdate}.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @JsonIgnore
    private Long version;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
