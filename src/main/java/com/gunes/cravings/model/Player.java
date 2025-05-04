package com.gunes.cravings.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data // Lombok: Getters, Setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: Parametresiz constructor
@AllArgsConstructor // Lombok: Tüm alanları içeren constructor
@Table(name = "players")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SQLite için IDENTITY genellikle çalışır
    private Long id;

    @Column(nullable = false, unique = true) // İsim boş olamaz ve benzersiz olmalı
    private String name;

    @Column(updatable = false)
    @CreationTimestamp // Otomatik oluşturulma zamanı
    private LocalDateTime createdAt;

    @UpdateTimestamp // Otomatik güncellenme zamanı
    private LocalDateTime updatedAt;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE") // Varsayılan olarak aktif
    private Boolean isActive = true;

    // Bir oyuncunun hangi dizilişlerde olduğunu görmek için (opsiyonel, gerekirse)
    // @OneToMany(mappedBy = "player", cascade = CascadeType.REMOVE)
    // private Set<LineupPosition> positions;
}
