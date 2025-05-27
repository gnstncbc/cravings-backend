package com.gunes.cravings.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMaxUsesRequestDTO {
    @NotNull(message = "Maximum uses cannot be null")
    @Min(value = 1, message = "Maximum uses must be at least 1")
    private Integer maxUses;
}