package com.gunes.cravings.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferralCodeGenerateRequestDTO {
    @Min(value = 1, message = "Maximum uses must be at least 1")
    private Integer maxUses = 1; // Default to 1 if not provided by client
}