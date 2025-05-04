package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineupPositionDTO { // Bir oyuncunun pozisyon detayı
    private Long playerId; // Frontend'in ID'yi bilmesi gerekebilir
    private String playerName; // İsmi göstermek için
    private Double xPercent;
    private Double yPercent;
}
