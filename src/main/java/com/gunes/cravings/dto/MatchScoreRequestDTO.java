package com.gunes.cravings.dto; // Paket adınızı kendi yapınıza göre düzenleyin

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreRequestDTO {

    @NotNull(message = "A Takımı skoru boş olamaz.")
    @Min(value = 0, message = "Skor negatif olamaz.")
    private Integer teamAScore;

    @NotNull(message = "B Takımı skoru boş olamaz.")
    @Min(value = 0, message = "Skor negatif olamaz.")
    private Integer teamBScore;
}