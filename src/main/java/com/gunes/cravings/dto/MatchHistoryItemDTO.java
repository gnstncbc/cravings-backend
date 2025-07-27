package com.gunes.cravings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchHistoryItemDTO {
    private Long matchId;
    private String matchName;
    private LocalDateTime matchDate;
    private Integer teamAScore;
    private Integer teamBScore;
    private String playerTeam; // "A" veya "B"
    private String result; // "WIN", "LOSS", "DRAW", "SCORE_PENDING"
    private Map<String, LineupPositionDTO> lineupA;
    private Map<String, LineupPositionDTO> lineupB;
}