package com.gunes.cravings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorcCreateRequestDTO {
    @JsonProperty("borc_name")
    @NotNull(message = "Borc name cannot be null")
    private String borcName; // Borç adı
    @JsonProperty("borc_amount")
    @NotNull(message = "Borc amount cannot be null")
    private Long borcAmount; // Borç miktarı
    @JsonProperty("is_paid")
    @NotNull(message = "Is paid cannot be null")
    private Boolean isPaid; // Borcun ödenip ödenmediği bilgisi
}
