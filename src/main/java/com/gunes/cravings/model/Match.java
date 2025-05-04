package com.gunes.cravings.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    @CreationTimestamp // Kaydedildiği zaman
    private LocalDateTime savedAt;

    private String matchName; // Opsiyonel maç/diziliş adı

    private String location; // Opsiyonel lokasyon

    // mappedBy: LineupPosition entity'sindeki 'match' alanı
    // cascade: Match silinince ilişkili LineupPosition'lar da silinsin
    // orphanRemoval: Match'in listesinden çıkarılan LineupPosition DB'den silinsin
    // fetch: LAZY olması performansı artırır, pozisyonlar sadece ihtiyaç duyulduğunda yüklenir
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude // Lombok @Data'nın sonsuz döngüye girmemesi için
    private Set<LineupPosition> lineupPositions = new HashSet<>(); // Null olmaması için initialize

    // Helper method (İlişkiyi çift taraflı yönetmek için)
    public void addLineupPosition(LineupPosition position) {
        this.lineupPositions.add(position);
        position.setMatch(this);
    }
}