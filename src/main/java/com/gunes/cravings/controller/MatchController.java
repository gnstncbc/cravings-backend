package com.gunes.cravings.controller;

import com.gunes.cravings.dto.MatchCreateRequestDTO;
import com.gunes.cravings.dto.MatchDetailDTO;
import com.gunes.cravings.dto.MatchSummaryDTO;
import com.gunes.cravings.dto.PredictionRequestDTO;
import com.gunes.cravings.dto.PredictionResponseDTO;
import com.gunes.cravings.dto.MatchScoreRequestDTO;
import com.gunes.cravings.dto.MatchScoreResponseDTO;
import com.gunes.cravings.dto.VoteRequestDTO;
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
@CrossOrigin(origins = "*") // Global CORS ayarınız korunuyor
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    public ResponseEntity<List<MatchSummaryDTO>> getAllMatchSummaries() {
        List<MatchSummaryDTO> summaries = matchService.getAllMatchSummaries();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDetailDTO> getMatchDetails(@PathVariable Long id) {
        // Bu metod artık skorları da içeren MatchDetailDTO dönecek
        MatchDetailDTO matchDetails = matchService.getMatchDetails(id);
        return ResponseEntity.ok(matchDetails);
    }

    @PostMapping
    public ResponseEntity<MatchDetailDTO> saveMatch(@Valid @RequestBody MatchCreateRequestDTO createDTO) {
        MatchDetailDTO savedMatch = matchService.saveMatch(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMatch);
    }

    // YENİ: Skor Kaydetme/Güncelleme Endpoint'i
    @PostMapping("/{matchId}/score")
    public ResponseEntity<MatchScoreResponseDTO> saveOrUpdateScore(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchScoreRequestDTO scoreRequestDTO) {
        MatchScoreResponseDTO response = matchService.saveOrUpdateMatchScore(matchId, scoreRequestDTO);
        // Başarılı durumda 200 OK veya skor yeni oluşturulduysa 201 CREATED
        // dönebilirsiniz.
        // Şimdilik OK dönüyoruz.
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/predict")
    public ResponseEntity<PredictionResponseDTO> predictWinner(@RequestBody PredictionRequestDTO request) {
        PredictionResponseDTO prediction = matchService.predictWinner(request);
        return ResponseEntity.ok(prediction);
    }
}