package com.gunes.cravings.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParaResponseDTO {
    private String message; // İsteğe bağlı mesaj
    @JsonProperty("remaining_money")
    private BigDecimal remainingMoney; // Kalan bakiye miktarı (opsiyonel)
}
