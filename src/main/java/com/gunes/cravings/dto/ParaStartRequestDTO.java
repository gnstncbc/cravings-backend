package com.gunes.cravings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParaStartRequestDTO {
    @JsonProperty("salary")
    private Long salary; // Maaş miktarı, örneğin "5000.00" gibi bir long olarak alıyoruz
    @JsonProperty("credit_card_total_limit")
    private Long creditCardTotalLimit; // Kredi kartı toplam limiti, örneğin "10000.00" gibi bir long olarak alıyoruz
    @JsonProperty("credit_card_remaining_limit")
    private Long creditCardRemainingLimit; // Kredi kartı kalan limiti, örneğin "8000.00" gibi bir long olarak alıyoru
}
