package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A physical seat position in a {@link Screen}'s fixed layout (e.g. row "A", seat 12,
 * PREMIUM). Every {@link Show} scheduled on this screen gets one {@link ShowSeat}
 * per SeatTemplate, generated at show-creation time.
 */
@Entity
@Table(name = "seat_templates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"screen_id", "row_label", "seat_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatTemplate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "row_label", nullable = false, length = 5)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType seatType;
}
