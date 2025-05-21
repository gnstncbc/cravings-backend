package com.gunes.cravings.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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

    // YENİ: PlayerStats ile birebir ilişki
    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @EqualsAndHashCode.Exclude // Lombok @Data ile döngüsel bağımlılığı önlemek için
    private PlayerStats playerStats;

    // Helper method for bidirectional association
    public void setPlayerStats(PlayerStats stats) {
        if (stats == null) {
            // Eğer mevcut bir playerStats varsa ve biz onu null'a çekiyorsak,
            // eski playerStats'ın player bağlantısını koparmak gerekir.
            if (this.playerStats != null) {
                this.playerStats.setPlayer(null); // DİKKAT: Bu satır eski playerStats'ın playerId'sini NULL yapar.
            }
        } else {
            // Yeni bir stats atanıyorsa, bu stats'ın player'ının 'this' (yani mevcut Player)
            // olduğundan ve playerId'sinin this.id olduğundan emin olmalıyız.
            stats.setPlayer(this); // Bu çağrı stats.playerId = this.id atamasını yapar.
        }
        this.playerStats = stats;
    }
}
