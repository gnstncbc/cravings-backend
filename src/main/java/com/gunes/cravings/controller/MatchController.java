package com.gunes.cravings.controller;

import com.gunes.cravings.dto.MatchCreateRequestDTO;
import com.gunes.cravings.dto.MatchDetailDTO;
import com.gunes.cravings.dto.MatchSummaryDTO;
import com.gunes.cravings.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Global CORS ayarı
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    public ResponseEntity<List<MatchSummaryDTO>> getAllMatchSummaries() {
        List<MatchSummaryDTO> summaries = matchService.getAllMatchSummaries();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDetailDTO> getMatchDetails(@PathVariable Long id) {
        MatchDetailDTO matchDetails = matchService.getMatchDetails(id);
        return ResponseEntity.ok(matchDetails);
    }

    @PostMapping
    public ResponseEntity<MatchDetailDTO> saveMatch(@Valid @RequestBody MatchCreateRequestDTO createDTO) {
        MatchDetailDTO savedMatch = matchService.saveMatch(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMatch);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}