package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerSelectDTO {
    private Long playerId;
    private String playerName;
    private String selectedTeam; // "A" or "B"
    private String selectorEmail; // Who selected this player
}