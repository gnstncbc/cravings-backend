package com.gunes.cravings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditCardLimitChangeRequestDTO {
    @NotNull(message = "Para miktarı boş olamaz.")
    @JsonProperty("new_credit_card_limit")
    private String limit; // Para miktarı, örneğin "100.00" gibi bir string olarak alıyoruz
}
