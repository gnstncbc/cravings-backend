package com.gunes.cravings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParaRequestDTO {
    @NotNull(message = "Para miktarı boş olamaz.")
    @JsonProperty("credit_card_remaining_limit")
    private String creditCardRemainingLimit; // Para miktarı, örneğin "100.00" gibi bir string olarak alıyoruz
}
