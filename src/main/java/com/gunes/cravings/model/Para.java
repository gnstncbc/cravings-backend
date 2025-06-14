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
@Table(name = "para")
public class Para {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Para tablosu için benzersiz bir ID
    //maaş
    private Long salary; // Maaş miktarı, örneğin "5000.00" gibi bir string olarak saklanabilir
    //private String remainingMoney; // Kalan bakiye miktarı, örneğin "4000.00" gibi bir string olarak saklanabilir    //credit card total limit
    private Long creditCardTotalLimit; // Kredi kartı toplam limiti, örneğin "10000.00" gibi bir string olarak saklanabilir
    private Long creditCardRemainingLimit; // Kredi kartı kalan limiti, örneğin "8000.00" gibi bir string olarak saklanabilir

}
