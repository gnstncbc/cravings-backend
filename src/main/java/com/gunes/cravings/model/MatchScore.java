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
@Table(name = "match_scores")
public class MatchScore {

    @Id
    private Long id; // Match ID'si ile aynı olacak (Paylaşılan Primary Key)

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "match_id")
    @EqualsAndHashCode.Exclude
    private Match match;

    @Column(name = "team_a_score")
    private Integer teamAScore;

    @Column(name = "team_b_score")
    private Integer teamBScore;

    // Maç ve skorları birlikte oluşturmak için yardımcı constructor
    public MatchScore(Match match, Integer teamAScore, Integer teamBScore) {
        this.match = match;
        this.id = match.getId(); // Match ID'sini bu entity'nin ID'si olarak ayarla
        this.teamAScore = teamAScore;
        this.teamBScore = teamBScore;
    }
}