package com.gunes.cravings.repository;

import com.gunes.cravings.model.LineupPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineupPositionRepository extends JpaRepository<LineupPosition, Long> {

    @Query("SELECT lp FROM LineupPosition lp JOIN FETCH lp.match m LEFT JOIN FETCH m.matchScore WHERE lp.player.id = :playerId ORDER BY m.savedAt DESC")
    List<LineupPosition> findByPlayerWithMatchDetails(@Param("playerId") Long playerId);

    @Query("SELECT lp FROM LineupPosition lp JOIN FETCH lp.player p JOIN FETCH lp.match m LEFT JOIN FETCH m.matchScore WHERE p.id IN :playerIds")
    List<LineupPosition> findByPlayerIdsWithDetails(@Param("playerIds") List<Long> playerIds);
}