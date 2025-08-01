package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerPairStatsDTO {
    private Long player1Id;
    private Long player2Id;
    private double winPercentage;
    private int gamesPlayedTogether;
}