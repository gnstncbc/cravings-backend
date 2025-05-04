package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchDetailDTO { // Maçın tüm detayları (yükleme için)
    private Long id;
    private LocalDateTime savedAt;
    private String matchName;
    private String location;
    private Map<String, LineupPositionDTO> lineupA = new HashMap<>(); // Key: Player ID (String), Value: Position
    private Map<String, LineupPositionDTO> lineupB = new HashMap<>(); // Key: Player ID (String), Value: Position
}