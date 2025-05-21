package com.gunes.cravings.repository;

import com.gunes.cravings.model.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {
    // Player ID'sine göre PlayerStats bulmak için özel bir metod eklenebilir,
    // ancak JpaRepository.findById(playerId) zaten bu işlevi görür.
    // Optional<PlayerStats> findByPlayerId(Long playerId); // Bu zaten findById ile aynı işi yapar.
}