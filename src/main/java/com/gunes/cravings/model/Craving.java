package com.gunes.cravings.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "cravings")
public class Craving {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int duration;  // Saniye cinsinden

    private int intensity; // 1-10 arasında
    private String mood;
    private String notes;
    private String wifiSsid;
    private LocalDateTime createdAt;

    public Craving() {
        this.createdAt = LocalDateTime.now();
    }

    // Getter ve Setter metodları
}
