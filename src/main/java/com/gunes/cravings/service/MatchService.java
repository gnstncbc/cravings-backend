package com.gunes.cravings.service;

import com.gunes.cravings.dto.*;
import com.gunes.cravings.model.LineupPosition;
import com.gunes.cravings.model.Match;
import com.gunes.cravings.model.Player;
import com.gunes.cravings.exception.ResourceNotFoundException;
import com.gunes.cravings.model.MatchScore; // YENİ IMPORT
import com.gunes.cravings.repository.MatchRepository;
import com.gunes.cravings.repository.PlayerRepository;
// MatchScoreRepository import'u eğer Match üzerinden yönetiliyorsa gerekmeyebilir.
// import com.gunes.cravings.repository.MatchScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository; // Player bulmak için
    // private final MatchScoreRepository matchScoreRepository; // Eğer direkt kullanılacaksa

    @Transactional(readOnly = true)
    public List<MatchSummaryDTO> getAllMatchSummaries() {
        return matchRepository.findAllByOrderBySavedAtDesc()
                .stream()
                .map(this::convertToMatchSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchDetailDTO getMatchDetails(Long id) {
        Match match = matchRepository.findByIdWithLineupPositions(id) // Bu özel sorgunuzu koruyoruz
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        return convertToMatchDetailDTO(match);
    }

    @Transactional
    public MatchDetailDTO saveMatch(MatchCreateRequestDTO createDTO) {
        Match match = new Match();
        match.setMatchName(createDTO.getMatchName());
        match.setLocation(createDTO.getLocation());

        processLineup(createDTO.getLineupA(), match, "A");
        processLineup(createDTO.getLineupB(), match, "B");

        // Yeni maç kaydedilirken skor eklenmez, bu ayrı bir işlemle yapılır.
        // match.setMatchScore(null); // Zaten yeni Match nesnesinde null olacaktır.

        Match savedMatch = matchRepository.save(match);
        return getMatchDetails(savedMatch.getId());
    }

    // YENİ: Skor Kaydetme/Güncelleme Servis Metodu
    @Transactional
    public MatchScoreResponseDTO saveOrUpdateMatchScore(Long matchId, MatchScoreRequestDTO scoreRequestDTO) {
        Match match = matchRepository.findById(matchId) // Skor güncellerken pozisyonları çekmeye gerek yok
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId + " to update score."));

        MatchScore matchScore = match.getMatchScore();
        if (matchScore == null) {
            matchScore = new MatchScore();
            // MatchScore entity'sindeki @MapsId ve Match entity'sindeki setMatchScore helper'ı
            // ID ve çift yönlü ilişkiyi doğru kurmalı.
            // matchScore.setId(match.getId()); // setMatchScore içinde yapılabilir veya @MapsId bunu yönetir
            // matchScore.setMatch(match);   // setMatchScore içinde yapılabilir
            match.setMatchScore(matchScore); // Bu helper metod ID ve match referansını ayarlamalı
        }

        matchScore.setTeamAScore(scoreRequestDTO.getTeamAScore());
        matchScore.setTeamBScore(scoreRequestDTO.getTeamBScore());

        // Match entity'si kaydedildiğinde, CascadeType.ALL sayesinde ilişkili MatchScore da kaydedilir/güncellenir.
        matchRepository.save(match);

        return new MatchScoreResponseDTO(matchId, matchScore.getTeamAScore(), matchScore.getTeamBScore(), "Skorlar başarıyla güncellendi.");
    }


    @Transactional
    public void deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        // CascadeType.ALL ve orphanRemoval=true sayesinde ilişkili LineupPosition'lar
        // ve MatchScore (eğer Match entity'sinde @OneToOne ilişkisinde cascade varsa) otomatik silinir.
        matchRepository.delete(match);
    }

    private void processLineup(Map<String, LineupPositionInputDTO> lineupMap, Match match, String teamIdentifier) {
        if (lineupMap != null) {
            for (Map.Entry<String, LineupPositionInputDTO> entry : lineupMap.entrySet()) {
                Long playerId;
                try {
                    playerId = Long.parseLong(entry.getKey());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid player ID format in request: " + entry.getKey());
                    continue;
                }

                LineupPositionInputDTO positionInput = entry.getValue();
                if (positionInput == null || positionInput.getXPercent() == null || positionInput.getYPercent() == null) {
                    System.err.println("Invalid position data for player ID: " + playerId);
                    continue;
                }

                Player player = playerRepository.findById(playerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Player not found for lineup position with id: " + playerId));

                LineupPosition position = new LineupPosition();
                position.setPlayer(player);
                position.setTeamIdentifier(teamIdentifier);
                position.setXPercent(positionInput.getXPercent());
                position.setYPercent(positionInput.getYPercent());

                match.addLineupPosition(position);
            }
        }
    }

    private MatchSummaryDTO convertToMatchSummaryDTO(Match match) {
        // SKORLARI ÖZET DTO'YA EKLEMEK İSTERSENİZ BURADA YAPABİLİRSİNİZ
        // Şimdilik orijinal haliyle bırakıyorum.
        return new MatchSummaryDTO(
                match.getId(),
                match.getSavedAt(),
                match.getMatchName(),
                match.getLocation()
        );
    }

    private MatchDetailDTO convertToMatchDetailDTO(Match match) {
        MatchDetailDTO detailDTO = new MatchDetailDTO(); // lineupA ve B map'leri initialize edilmiş olmalı
        detailDTO.setId(match.getId());
        detailDTO.setSavedAt(match.getSavedAt());
        detailDTO.setMatchName(match.getMatchName());
        detailDTO.setLocation(match.getLocation());

        // YENİ: Skor bilgilerini DTO'ya ekle
        if (match.getMatchScore() != null) {
            detailDTO.setTeamAScore(match.getMatchScore().getTeamAScore());
            detailDTO.setTeamBScore(match.getMatchScore().getTeamBScore());
        } else {
            detailDTO.setTeamAScore(null); // Veya frontend'in beklediği bir varsayılan (örn. 0)
            detailDTO.setTeamBScore(null);
        }

        Set<LineupPosition> positions = match.getLineupPositions();
        if (positions != null) {
            for (LineupPosition pos : positions) {
                Player player = pos.getPlayer();
                if (player == null) continue;

                // Mevcut LineupPositionDTO kullanımınız korunuyor.
                // Bu DTO'nun (playerId, playerName, xPercent, yPercent) constructor'ı olduğu varsayılıyor.
                LineupPositionDTO posDTO = new LineupPositionDTO(
                        player.getId(),
                        player.getName(),
                        pos.getXPercent(),
                        pos.getYPercent()
                );

                if ("A".equalsIgnoreCase(pos.getTeamIdentifier())) {
                    detailDTO.getLineupA().put(String.valueOf(player.getId()), posDTO);
                } else if ("B".equalsIgnoreCase(pos.getTeamIdentifier())) {
                    detailDTO.getLineupB().put(String.valueOf(player.getId()), posDTO);
                }
            }
        }
        return detailDTO;
    }
}