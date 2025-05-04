package com.gunes.cravings.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lineup_positions")
public class LineupPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fetch: LAZY olması önerilir
    // JoinColumn: Foreign key kolonunun adını belirtir
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bir pozisyon mutlaka bir oyuncuya ait olmalı
    @JoinColumn(name = "player_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bir pozisyon mutlaka bir maça ait olmalı
    @JoinColumn(name = "match_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Match match;

    @Column(nullable = false)
    private String teamIdentifier; // "A" veya "B"

    @Column(nullable = false)
    private Double coordinateX;

    @Column(nullable = false)
    private Double coordinateY;
}
