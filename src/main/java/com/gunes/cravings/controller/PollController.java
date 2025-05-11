package com.gunes.cravings.controller;

import com.gunes.cravings.dto.MatchDetailDTO;
import com.gunes.cravings.dto.VoteRequestDTO;
import com.gunes.cravings.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gunes.cravings.dto.UserVoteDTO;
import java.util.List;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Consider if you need global CORS or specific for this
public class PollController {

    private final PollService pollService;

    @PostMapping("/{matchId}/vote")
    public ResponseEntity<MatchDetailDTO> submitVote(
            @PathVariable Long matchId,
            @Valid @RequestBody VoteRequestDTO voteRequest) {
        MatchDetailDTO updatedMatchDetails = pollService.submitVote(matchId, voteRequest);
        return ResponseEntity.ok(updatedMatchDetails);
    }

    @GetMapping("/{matchId}/results")
    public ResponseEntity<MatchDetailDTO> getMatchResults(@PathVariable Long matchId) {
        MatchDetailDTO matchDetails = pollService.getMatchResults(matchId);
        return ResponseEntity.ok(matchDetails);
    }

    @GetMapping("/{matchId}/user-votes")
    public ResponseEntity<List<UserVoteDTO>> getVotesForMatchByUsers(@PathVariable Long matchId) {
        List<UserVoteDTO> userVotes = pollService.getVotesForMatchByUsers(matchId);
        return ResponseEntity.ok(userVotes);
    }
} 