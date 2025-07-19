package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamSelectionStatusDTO {
    private String roomCode;
    private String currentSelectorEmail; // Email of the captain currently selecting
    private String nextSelectorEmail; // Email of the captain next in line
    private String selectionPhase; // E.g., "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    private Map<String, PlayerSelectDTO> teamASelections; // PlayerId -> PlayerSelectDTO
    private Map<String, PlayerSelectDTO> teamBSelections; // PlayerId -> PlayerSelectDTO
    private List<PlayerDTO> availablePlayers; // Players not yet selected
    private int teamASize;
    private int teamBSize;
    private String lastSelectedPlayerName; // Name of the last player selected
    private String lastSelectedTeam; // Team of the last player selected
}