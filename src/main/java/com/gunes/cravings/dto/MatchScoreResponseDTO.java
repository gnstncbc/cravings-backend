package com.gunes.cravings.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreResponseDTO {
    private Long matchId;
    private Integer teamAScore;
    private Integer teamBScore;
    private String message; // Ekstra bilgi mesajı için
}