package com.gunes.cravings.repository; // Paket adınızı kendi yapınıza göre düzenleyin

import com.gunes.cravings.model.MatchScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchScoreRepository extends JpaRepository<MatchScore, Long> {
    // Özel sorgular gerekirse buraya eklenebilir.
    // Örneğin, Match ID'sine göre MatchScore bulmak için:
    // findByMatchId(Long matchId) -> Zaten JpaRepository.findById(matchId) bunu yapar.
}