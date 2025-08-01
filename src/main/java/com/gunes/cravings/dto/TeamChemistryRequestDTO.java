package com.gunes.cravings.dto;

import lombok.Data;
import java.util.List;

@Data
public class TeamChemistryRequestDTO {
    private List<Long> playerIds;
}