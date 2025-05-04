package com.gunes.cravings.service;

import com.gunes.cravings.dto.*;
import com.gunes.cravings.model.LineupPosition;
import com.gunes.cravings.model.Match;
import com.gunes.cravings.model.Player;
import com.gunes.cravings.exception.ResourceNotFoundException;
import com.gunes.cravings.repository.MatchRepository;
import com.gunes.cravings.repository.PlayerRepository;
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

    @Transactional(readOnly = true)
    public List<MatchSummaryDTO> getAllMatchSummaries() {
        return matchRepository.findAllByOrderBySavedAtDesc()
                .stream()
                .map(this::convertToMatchSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchDetailDTO getMatchDetails(Long id) {
        // Pozisyonları ve oyuncu bilgilerini de çekmek için özel sorguyu kullanabiliriz
        Match match = matchRepository.findByIdWithLineupPositions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        return convertToMatchDetailDTO(match);
    }

    @Transactional
    public MatchDetailDTO saveMatch(MatchCreateRequestDTO createDTO) {
        Match match = new Match();
        match.setMatchName(createDTO.getMatchName());
        match.setLocation(createDTO.getLocation());
        // savedAt @CreationTimestamp ile otomatik dolacak

        // Lineup A işle
        processLineup(createDTO.getLineupA(), match, "A");

        // Lineup B işle
        processLineup(createDTO.getLineupB(), match, "B");

        Match savedMatch = matchRepository.save(match);
        // Kaydedilmiş maçı tekrar çekerek tüm ilişkilerin yüklendiğinden emin olalım
        return getMatchDetails(savedMatch.getId());
    }


    @Transactional
    public void deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        // CascadeType.ALL ve orphanRemoval=true sayesinde ilişkili LineupPosition'lar otomatik silinir.
        matchRepository.delete(match);
    }

    // Helper: Gelen lineup map'ini işleyip Match entity'sine ekler
    private void processLineup(Map<String, LineupPositionInputDTO> lineupMap, Match match, String teamIdentifier) {
        if (lineupMap != null) {
            for (Map.Entry<String, LineupPositionInputDTO> entry : lineupMap.entrySet()) {
                Long playerId;
                try {
                    playerId = Long.parseLong(entry.getKey());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid player ID format in request: " + entry.getKey());
                    continue; // veya hata fırlat
                }

                LineupPositionInputDTO positionInput = entry.getValue();
                if (positionInput == null || positionInput.getX() == null || positionInput.getY() == null) {
                     System.err.println("Invalid position data for player ID: " + playerId);
                     continue; // veya hata fırlat
                }

                // Player'ı bul
                Player player = playerRepository.findById(playerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Player not found for lineup position with id: " + playerId));

                // Yeni LineupPosition oluştur
                LineupPosition position = new LineupPosition();
                position.setPlayer(player);
                // position.setMatch(match); // addLineupPosition içinde set ediliyor
                position.setTeamIdentifier(teamIdentifier);
                position.setCoordinateX(positionInput.getX());
                position.setCoordinateY(positionInput.getY());

                // Match entity'sine ekle (ilişkiyi kurmak için helper methodu kullan)
                match.addLineupPosition(position);
            }
        }
    }


    // --- DTO Dönüşüm Metodları ---

    private MatchSummaryDTO convertToMatchSummaryDTO(Match match) {
        return new MatchSummaryDTO(
                match.getId(),
                match.getSavedAt(),
                match.getMatchName(),
                match.getLocation()
        );
    }

    private MatchDetailDTO convertToMatchDetailDTO(Match match) {
        MatchDetailDTO detailDTO = new MatchDetailDTO();
        detailDTO.setId(match.getId());
        detailDTO.setSavedAt(match.getSavedAt());
        detailDTO.setMatchName(match.getMatchName());
        detailDTO.setLocation(match.getLocation());

        Set<LineupPosition> positions = match.getLineupPositions();
        if (positions != null) {
            for (LineupPosition pos : positions) {
                Player player = pos.getPlayer(); // LAZY fetch ise burada yüklenir
                 if (player == null) continue; // Eğer oyuncu silinmişse vb. durumlar için kontrol

                 LineupPositionDTO posDTO = new LineupPositionDTO(
                        player.getId(),
                        player.getName(), // Oyuncu ismini de ekleyelim
                        pos.getCoordinateX(),
                        pos.getCoordinateY()
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