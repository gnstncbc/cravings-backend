package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSelectDTO {
    private String playerId;
    private String teamIdentifier; // "A" or "B"
}