package com.gunes.cravings.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "player_stats")
public class PlayerStats {

    @Id
    private Long playerId; // Player ID'si ile aynı olacak (Paylaşılan Primary Key)

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @EqualsAndHashCode.Exclude
    @MapsId // Bu anotasyon, playerId'nin Player entity'sinin ID'si ile eşleşmesini sağlar
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "win_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer winCount = 0;

    @Column(name = "lose_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer loseCount = 0;

    @Column(name = "draw_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer drawCount = 0;

    // İlişkiyi player tarafından kurmak için yardımcı metod
    public void setPlayer(Player player) {
        this.player = player;
        if (player != null) {
            // Player nesnesinin ID'si null ise burada sorun başlar.
            // Bu metod çağrıldığında player.getId() kesinlikle dolu olmalı.
            this.playerId = player.getId();
        } else {
            // Bu else bloğu normalde @MapsId için sorunlu olabilir,
            // çünkü PlayerStats'ın var olabilmesi için bir Player'a bağlı olması gerekir.
            // Eğer bir PlayerStats nesnesinden Player ilişkisi kaldırılıyorsa
            // ve orphanRemoval=true ise, PlayerStats silinir.
            // Eğer silinmiyorsa ve Player null ise, playerId null olur ve save edilemez.
            this.playerId = null;
        }
    }
}