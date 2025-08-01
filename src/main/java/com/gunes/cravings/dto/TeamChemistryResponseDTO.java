package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamChemistryResponseDTO {
    private double teamChemistry;
    private List<PlayerPairStatsDTO> playerPairStats;
}