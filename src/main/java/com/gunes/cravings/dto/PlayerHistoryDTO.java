package com.gunes.cravings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerHistoryDTO {
    private Long playerId;
    private String playerName;
    private int winCount;
    private int drawCount;
    private int loseCount;
    private int totalGames;
    private double winPercentage;
    private String resultSequence;
    private List<MatchHistoryItemDTO> matchHistory;
}