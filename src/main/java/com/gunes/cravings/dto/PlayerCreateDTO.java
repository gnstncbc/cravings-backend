package com.gunes.cravings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCreateDTO { // Yeni oyuncu ekleme isteği
    @NotBlank(message = "Oyuncu adı boş olamaz")
    @Size(min = 2, max = 50, message = "Oyuncu adı 2 ile 50 karakter arasında olmalı")
    private String name;
}
