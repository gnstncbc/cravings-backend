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
    @CreationTimestamp
    private LocalDateTime savedAt;

    private String matchName;

    private String location;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private Set<LineupPosition> lineupPositions = new HashSet<>();

    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private MatchScore matchScore;

    // Helper method: LineupPosition eklemek için (Mevcut)
    public void addLineupPosition(LineupPosition position) {
        this.lineupPositions.add(position);
        position.setMatch(this);
    }

    // Helper method: MatchScore'u ayarlamak ve çift yönlü ilişkiyi doğru kurmak için
    public void setMatchScore(MatchScore score) {
        if (score == null) { // Eğer gelen skor null ise
            if (this.matchScore != null) {
                this.matchScore.setMatch(null); // Eski skorun match referansını temizle
            }
        } else { // Eğer yeni bir skor geldiyse
            score.setMatch(this); // Yeni skorun match referansını bu maça ayarla
            if (this.id != null) { // Eğer bu maçın ID'si varsa (kaydedilmişse)
                score.setId(this.id); // Yeni skorun ID'sini bu maçın ID'si yap (@MapsId için önemli)
            }
        }
        this.matchScore = score; // Bu maçın skorunu yeni skorla güncelle
    }

    // JPA entity lifecycle callback'i: ID atandıktan sonra MatchScore'un ID'sini de ayarlamak için.
    // Bu, Match yeni oluşturulup hemen ardından MatchScore set edilirse ve sonra Match kaydedilirse
    // MatchScore'un ID'sinin doğru set edilmesini garantiler.
    @PostPersist
    @PostUpdate
    private void ensureMatchScoreId() {
        if (this.matchScore != null && this.id != null) {
            if (this.matchScore.getId() == null || !this.matchScore.getId().equals(this.id)) {
                 this.matchScore.setId(this.id); // MatchScore'un ID'sini Match'in ID'si ile senkronize et
                 // Eğer MatchScore'u ayrıca kaydetmiyorsanız ve CascadeType.ALL kullanıyorsanız,
                 // bu aşamada bir entityManager.merge(this.matchScore) gerekebilir,
                 // ancak genellikle cascade bu durumu yönetir. Test etmekte fayda var.
                 // Daha güvenli bir yol, MatchScore nesnesini oluştururken Match'in ID'sinin zaten var olmasını sağlamaktır.
            }
            if (this.matchScore.getMatch() == null || !this.matchScore.getMatch().equals(this)) {
                this.matchScore.setMatch(this); // Match referansını da senkronize et
            }
        }
    }
}