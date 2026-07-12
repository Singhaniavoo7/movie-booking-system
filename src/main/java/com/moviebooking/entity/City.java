package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cities", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "state"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String state;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
