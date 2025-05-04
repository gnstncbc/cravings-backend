package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchSummaryDTO { // Maç listesi için özet bilgi
    private Long id;
    private LocalDateTime savedAt;
    private String matchName;
    private String location;
    // private int teamACount; // Gerekirse oyuncu sayıları eklenebilir
    // private int teamBCount;
}
