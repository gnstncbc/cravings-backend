package com.gunes.cravings.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data // Getter ve Setter'ları otomatik oluşturur
@AllArgsConstructor // Tüm alanları içeren bir constructor oluşturur
@NoArgsConstructor
public class LineupPositionInputDTO { // Maç kaydederken gelen pozisyon bilgisi
    @NotNull // X ve Y boş olamaz
    @JsonProperty("xPercent")
    private Double xPercent;

    @NotNull
    @JsonProperty("yPercent")
    private Double yPercent;
     // Frontend zaten ismi biliyor, sadece koordinat yeterli
}
