package com.gunes.cravings.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineupPositionInputDTO { // Maç kaydederken gelen pozisyon bilgisi
    @NotNull // X ve Y boş olamaz
    private Double x;
    @NotNull
    private Double y;
     // Frontend zaten ismi biliyor, sadece koordinat yeterli
}
