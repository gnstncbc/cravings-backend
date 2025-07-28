package com.gunes.cravings.dto;

import lombok.Data;
import java.util.List;

@Data
public class PredictionRequestDTO {
    private List<Long> teamAPlayerIds;
    private List<Long> teamBPlayerIds;
}