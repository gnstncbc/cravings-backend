package com.gunes.cravings.repository;

import com.gunes.cravings.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    // Kayıtlı maçları tarih sırasına göre getir
    List<Match> findAllByOrderBySavedAtDesc();

    // Bir maçı pozisyonlarıyla birlikte çekmek için (FetchType.LAZY kullanılıyorsa gerekli olabilir)
    @Query("SELECT m FROM Match m LEFT JOIN FETCH m.lineupPositions lp LEFT JOIN FETCH lp.player WHERE m.id = :id")
    Optional<Match> findByIdWithLineupPositions(Long id);

    // YENİ METOT: Skoru olan ve lineup pozisyonları olan maçları getirir
    @Query("SELECT DISTINCT m FROM Match m JOIN FETCH m.matchScore ms JOIN FETCH m.lineupPositions lp JOIN FETCH lp.player WHERE ms.teamAScore IS NOT NULL AND ms.teamBScore IS NOT NULL")
    List<Match> findAllWithScoresAndLineups();
}
