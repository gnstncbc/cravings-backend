package com.gunes.cravings.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PredictionResponseDTO {
    private double teamAWinPercentage;
    private double teamBWinPercentage;
}