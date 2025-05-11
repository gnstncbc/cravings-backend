package com.gunes.cravings.repository;

import com.gunes.cravings.model.Match;
import com.gunes.cravings.model.User;
import com.gunes.cravings.model.UserVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserVoteRepository extends JpaRepository<UserVote, Long> {
    Optional<UserVote> findByUserAndMatch(User user, Match match);
    List<UserVote> findAllByMatch(Match match);
    // You can add other query methods if needed, e.g., count votes for a match by type
} 