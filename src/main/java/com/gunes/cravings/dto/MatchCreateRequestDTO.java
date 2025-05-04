package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchCreateRequestDTO { // Maç kaydetme isteği için
    private String matchName;
    private String location;
    private Map<String, LineupPositionInputDTO> lineupA; // Key: Player ID (String)
    private Map<String, LineupPositionInputDTO> lineupB; // Key: Player ID (String)
}