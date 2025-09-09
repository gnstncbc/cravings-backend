package com.gunes.cravings.model;

import jakarta.annotation.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "borc")
public class Borc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Para tablosu için benzersiz bir ID
    private String borcName; // Borç adı
    private Long borcAmount; // Borç miktarı
    private Boolean isPaid; // Borcun ödenip ödenmediği bilgisi

}
