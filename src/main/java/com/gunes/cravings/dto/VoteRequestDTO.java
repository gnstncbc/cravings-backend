package com.gunes.cravings.dto;

import com.gunes.cravings.model.VoteType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequestDTO {
    @NotNull(message = "Vote type cannot be null")
    private VoteType voteType;
} 