package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatusDTO {
    private String roomCode;
    private List<String> usersInRoom;
    private String teamACaptainEmail;
    private String teamBCaptainEmail;
    private boolean selectionInProgress;
    private List<PlayerDTO> availablePlayersForSelection;
    private Map<String, PlayerDTO> teamASelectedPlayers;
    private Map<String, PlayerDTO> teamBSelectedPlayers;
    private String currentPlayerSelectionTurnEmail;
    private String selectionStatusMessage;
}