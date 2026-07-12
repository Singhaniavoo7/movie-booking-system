package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row per (show, physical seat). This is the row we take a pessimistic lock on
 * during hold/booking so that concurrent requests for the same seat are serialized
 * by the database rather than racing in application code. {@code status} plus
 * {@code holdExpiresAt} implement the time-bound hold: a HELD seat whose
 * {@code holdExpiresAt} is in the past is treated as available again, both lazily
 * (checked at read/lock time) and via the periodic sweep in
 * {@code SeatHoldExpiryScheduler}.
 */
@Entity
@Table(name = "show_seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_template_id"}),
        indexes = {
                @Index(name = "idx_show_seats_show_status", columnList = "show_id,status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_template_id", nullable = false)
    private SeatTemplate seatTemplate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ShowSeatStatus status = ShowSeatStatus.AVAILABLE;

    /** Price snapshot taken from the show's PricingTier x seat type at show-creation time. */
    @Column(nullable = false)
    private BigDecimal price;

    /** Set while HELD; null otherwise. */
    private Instant holdExpiresAt;

    /** User who currently holds/booked the seat; null when AVAILABLE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_user_id")
    private User heldByUser;

    /** True if a HELD row's hold window has passed and it should be treated as AVAILABLE. */
    @Transient
    public boolean isHoldExpired(Instant now) {
        return status == ShowSeatStatus.HELD && holdExpiresAt != null && holdExpiresAt.isBefore(now);
    }
}
