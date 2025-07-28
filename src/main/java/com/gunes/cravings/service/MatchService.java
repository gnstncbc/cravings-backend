package com.gunes.cravings.service;

import com.gunes.cravings.dto.*;
import com.gunes.cravings.model.*; // PlayerStats, LineupPosition için
import com.gunes.cravings.exception.ResourceNotFoundException;
import com.gunes.cravings.repository.MatchRepository;
import com.gunes.cravings.repository.PlayerRepository;
import com.gunes.cravings.repository.PlayerStatsRepository; // YENİ IMPORT
// LineupPositionRepository'ye doğrudan ihtiyaç olmayabilir eğer Match üzerinden erişiliyorsa
// import com.gunes.cravings.repository.LineupPositionRepository; 
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
    private final PlayerStatsRepository playerStatsRepository; // YENİ: PlayerStatsRepository enjekte edildi
    // private final LineupPositionRepository lineupPositionRepository; // Eğer
    // Match entity'sinden lineup'a ulaşılamıyorsa

    @Transactional(readOnly = true)
    public List<MatchSummaryDTO> getAllMatchSummaries() {
        return matchRepository.findAllByOrderBySavedAtDesc()
                .stream()
                .map(this::convertToMatchSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchDetailDTO getMatchDetails(Long id) {
        Match match = matchRepository.findByIdWithLineupPositions(id) // Bu özel sorgunuz lineup ve player'ları fetch
                                                                      // ediyor
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
        // getMatchDetails, lineup'ları ve player'ları da içeren DTO'yu döndürür.
        return getMatchDetails(savedMatch.getId());
    }

    @Transactional
    public MatchScoreResponseDTO saveOrUpdateMatchScore(Long matchId, MatchScoreRequestDTO scoreRequestDTO) {
        Match match = matchRepository.findByIdWithLineupPositions(matchId) // lineupPositions'ı da çekiyoruz.
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Match not found with id: " + matchId + " to update score."));

        MatchScore matchScore = match.getMatchScore();
        boolean isNewScore = false;
        if (matchScore == null) {
            matchScore = new MatchScore();
            isNewScore = true;
            // matchScore.setId(match.getId()); // @MapsId ve Match entity'sindeki helper
            // bunu yönetmeli
        }

        // TODO: Eğer skor değişiyorsa, eski istatistikleri geri almak/düzeltmek için
        // bir mantık eklenebilir.
        // Bu örnekte, her skor kaydında mevcut duruma göre istatistikler güncellenir.
        // Eğer bir maçın sonucu (örn: A kazanmıştı, sonra B kazandı olarak değişti)
        // tamamen değişiyorsa, bu basit artırma/azaltma mantığı yeterli olmayabilir.
        // Şimdilik, sadece kazanan/kaybeden sayısını artırıyoruz.

        matchScore.setTeamAScore(scoreRequestDTO.getTeamAScore());
        matchScore.setTeamBScore(scoreRequestDTO.getTeamBScore());

        if (isNewScore) {
            match.setMatchScore(matchScore); // Bu helper metod ID ve match referansını ayarlamalı
        }

        // Önce PlayerStats'ı güncelle, sonra maçı (ve skoru cascade ile) kaydet.
        updatePlayerStatsForMatchResult(match, scoreRequestDTO.getTeamAScore(), scoreRequestDTO.getTeamBScore());

        matchRepository.save(match); // Match'i kaydetmek, CascadeType.ALL ile MatchScore'u da kaydeder/günceller.

        return new MatchScoreResponseDTO(matchId, matchScore.getTeamAScore(), matchScore.getTeamBScore(),
                "Skorlar başarıyla güncellendi ve oyuncu istatistikleri işlendi.");
    }

    @Transactional
    public void deleteMatch(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + id));
        // CascadeType.ALL ve orphanRemoval=true sayesinde ilişkili LineupPosition'lar
        // ve MatchScore (eğer Match entity'sinde @OneToOne ilişkisinde cascade varsa)
        // otomatik silinir.
        matchRepository.delete(match);
    }

    private void processLineup(Map<String, LineupPositionInputDTO> lineupMap, Match match, String teamIdentifier) {
        if (lineupMap != null) {
            for (Map.Entry<String, LineupPositionInputDTO> entry : lineupMap.entrySet()) {
                Long playerId;
                try {
                    playerId = Long.parseLong(entry.getKey());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid player ID format in request: " + entry.getKey() + " for match: "
                            + match.getMatchName());
                    continue;
                }

                LineupPositionInputDTO positionInput = entry.getValue();
                if (positionInput == null || positionInput.getXPercent() == null
                        || positionInput.getYPercent() == null) {
                    System.err.println(
                            "Invalid position data for player ID: " + playerId + " for match: " + match.getMatchName());
                    continue;
                }

                Player player = playerRepository.findById(playerId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Player not found for lineup position with id: " + playerId));

                LineupPosition position = new LineupPosition();
                position.setPlayer(player);
                position.setTeamIdentifier(teamIdentifier);
                position.setXPercent(positionInput.getXPercent());
                position.setYPercent(positionInput.getYPercent());
                // position.setMatch(match); // Bu, match.addLineupPosition içinde yapılmalı.

                match.addLineupPosition(position); // Match entity'sindeki helper bu pozisyonu ekler ve
                                                   // position.setMatch(this) yapar.
            }
        }
    }

    // YENİ: Maç sonucuna göre oyuncu istatistiklerini güncelleyen metod
    @Transactional // Bu metodun da transactional olması iyi bir pratiktir.
    protected void updatePlayerStatsForMatchResult(Match match, int teamAScore, int teamBScore) {
        Set<LineupPosition> positions = match.getLineupPositions();
        if (positions == null || positions.isEmpty()) {
            System.err.println(
                    "No lineup positions found for match ID: " + match.getId() + ". Skipping player stats update.");
            return;
        }

        List<Player> teamAPlayers = positions.stream()
                .filter(lp -> "A".equalsIgnoreCase(lp.getTeamIdentifier()) && lp.getPlayer() != null)
                .map(LineupPosition::getPlayer)
                .collect(Collectors.toList());

        List<Player> teamBPlayers = positions.stream()
                .filter(lp -> "B".equalsIgnoreCase(lp.getTeamIdentifier()) && lp.getPlayer() != null)
                .map(LineupPosition::getPlayer)
                .collect(Collectors.toList());

        if (teamAScore > teamBScore) { // A Takımı kazandı
            incrementWinCounts(teamAPlayers);
            incrementLoseCounts(teamBPlayers);
        } else if (teamBScore > teamAScore) { // B Takımı kazandı
            incrementWinCounts(teamBPlayers);
            incrementLoseCounts(teamAPlayers);
        } else { // Beraberlik
            incrementDrawCounts(teamAPlayers);
            incrementDrawCounts(teamBPlayers);
        }
    }

    private void incrementWinCounts(List<Player> players) {
        for (Player player : players) {
            PlayerStats stats = playerStatsRepository.findById(player.getId())
                    .orElseGet(() -> {
                        // Bu durumun olmaması beklenir, çünkü her oyuncu oluşturulduğunda PlayerStats'ı
                        // da oluşmalı.
                        // Eğer bir şekilde oluşmamışsa, hata loglanır ve yeni bir stats oluşturulur.
                        System.err.println("CRITICAL: PlayerStats not found for player ID: " + player.getId()
                                + " during win update. Creating a new one.");
                        PlayerStats newStats = new PlayerStats();
                        newStats.setPlayer(player); // PlayerStats'daki setPlayer metodu playerId'yi de set etmeli.
                        // newStats.setWinCount(0); // Zaten default 0 olacak.
                        // newStats.setLoseCount(0);
                        return newStats; // Bu yeni stats daha sonra kaydedilecek.
                    });
            stats.setWinCount(stats.getWinCount() + 1);
            playerStatsRepository.save(stats);
        }
    }

    private void incrementLoseCounts(List<Player> players) {
        for (Player player : players) {
            PlayerStats stats = playerStatsRepository.findById(player.getId())
                    .orElseGet(() -> {
                        System.err.println("CRITICAL: PlayerStats not found for player ID: " + player.getId()
                                + " during lose update. Creating a new one.");
                        PlayerStats newStats = new PlayerStats();
                        newStats.setPlayer(player);
                        return newStats;
                    });
            stats.setLoseCount(stats.getLoseCount() + 1);
            playerStatsRepository.save(stats);
        }
    }

    private void incrementDrawCounts(List<Player> players) {
        for (Player player : players) {
            PlayerStats stats = playerStatsRepository.findById(player.getId())
                    .orElseGet(() -> {
                        System.err.println("CRITICAL: PlayerStats not found for player ID: " + player.getId()
                                + " during draw update. Creating a new one.");
                        PlayerStats newStats = new PlayerStats();
                        newStats.setPlayer(player);
                        return newStats;
                    });
            stats.setDrawCount(stats.getDrawCount() + 1);
            playerStatsRepository.save(stats);
        }
    }

    private MatchSummaryDTO convertToMatchSummaryDTO(Match match) {
        return new MatchSummaryDTO(
                match.getId(),
                match.getSavedAt(),
                match.getMatchName(),
                match.getLocation());
    }

    public MatchDetailDTO convertToMatchDetailDTO(Match match) { // Public yaptık çünkü PollService'de de kullanılıyor
                                                                 // olabilir.
        MatchDetailDTO detailDTO = new MatchDetailDTO();
        detailDTO.setId(match.getId());
        detailDTO.setSavedAt(match.getSavedAt());
        detailDTO.setMatchName(match.getMatchName());
        detailDTO.setLocation(match.getLocation());

        if (match.getMatchScore() != null) {
            detailDTO.setTeamAScore(match.getMatchScore().getTeamAScore());
            detailDTO.setTeamBScore(match.getMatchScore().getTeamBScore());
        } else {
            detailDTO.setTeamAScore(null);
            detailDTO.setTeamBScore(null);
        }

        detailDTO.setTeamAVotes(match.getTeamAVotes());
        detailDTO.setTeamBVotes(match.getTeamBVotes());
        detailDTO.setDrawVotes(match.getDrawVotes());

        Set<LineupPosition> positions = match.getLineupPositions(); // Zaten fetch edilmiş olmalı
        if (positions != null) {
            for (LineupPosition pos : positions) {
                Player player = pos.getPlayer();
                if (player == null)
                    continue;

                LineupPositionDTO posDTO = new LineupPositionDTO(
                        player.getId(),
                        player.getName(),
                        pos.getXPercent(),
                        pos.getYPercent());

                if ("A".equalsIgnoreCase(pos.getTeamIdentifier())) {
                    detailDTO.getLineupA().put(String.valueOf(player.getId()), posDTO);
                } else if ("B".equalsIgnoreCase(pos.getTeamIdentifier())) {
                    detailDTO.getLineupB().put(String.valueOf(player.getId()), posDTO);
                }
            }
        }
        return detailDTO;
    }

    public PredictionResponseDTO predictWinner(PredictionRequestDTO request) {
        List<Long> teamAPlayerIds = request.getTeamAPlayerIds();
        List<Long> teamBPlayerIds = request.getTeamBPlayerIds();

        if ((teamAPlayerIds == null || teamAPlayerIds.isEmpty())
                && (teamBPlayerIds == null || teamBPlayerIds.isEmpty())) {
            return PredictionResponseDTO.builder().teamAWinPercentage(50.0).teamBWinPercentage(50.0).build();
        }

        double totalPowerA = calculateTeamPower(teamAPlayerIds);
        double totalPowerB = calculateTeamPower(teamBPlayerIds);

        double combinedPower = totalPowerA + totalPowerB;

        if (combinedPower == 0) {
            return PredictionResponseDTO.builder().teamAWinPercentage(50.0).teamBWinPercentage(50.0).build();
        }

        double teamAWinPercentage = (totalPowerA / combinedPower) * 100;
        double teamBWinPercentage = (totalPowerB / combinedPower) * 100;

        return PredictionResponseDTO.builder()
                .teamAWinPercentage(teamAWinPercentage)
                .teamBWinPercentage(teamBWinPercentage)
                .build();
    }

    private double calculateTeamPower(List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return 0.0;
        }

        return playerIds.stream()
                .mapToDouble(this::getPlayerPowerScore)
                .sum();
    }

    private double getPlayerPowerScore(Long playerId) {
        final double DEFAULT_POWER_SCORE = 0.4; // Default score for players with no history (40% win rate equivalent)

        return playerStatsRepository.findById(playerId)
                .map(stats -> {
                    int totalGames = stats.getWinCount() + stats.getLoseCount() + stats.getDrawCount();
                    if (totalGames == 0 || totalGames < 3) {
                        return DEFAULT_POWER_SCORE;
                    }
                    return (double) stats.getWinCount() / totalGames;
                })
                .orElse(DEFAULT_POWER_SCORE);
    }
}