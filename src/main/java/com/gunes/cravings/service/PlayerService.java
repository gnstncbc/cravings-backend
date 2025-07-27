package com.gunes.cravings.service;

import com.gunes.cravings.model.LineupPosition;
import com.gunes.cravings.model.Match;
import com.gunes.cravings.dto.LineupPositionDTO;
import com.gunes.cravings.dto.MatchHistoryItemDTO;
import com.gunes.cravings.dto.PlayerCreateDTO;
import com.gunes.cravings.dto.PlayerDTO;
import com.gunes.cravings.dto.PlayerHistoryDTO;
import com.gunes.cravings.model.MatchScore;
import com.gunes.cravings.model.Player;
import com.gunes.cravings.model.PlayerStats; // YENİ IMPORT
import com.gunes.cravings.exception.ResourceAlreadyExistsException;
import com.gunes.cravings.exception.ResourceNotFoundException;
import com.gunes.cravings.repository.LineupPositionRepository;
import com.gunes.cravings.repository.MatchRepository;
import com.gunes.cravings.repository.PlayerRepository;
import com.gunes.cravings.repository.PlayerStatsRepository; // YENİ IMPORT
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository; // YENİ: PlayerStatsRepository enjekte edildi
    private final MatchRepository matchRepository;
    private final LineupPositionRepository lineupPositionRepository;
    private final MatchService matchService; // YENİ: MatchService enjekte edildi

    // Tüm aktif oyuncuları getir (veya tümü, ihtiyaca göre)
    @Transactional(readOnly = true) // Veri okuma işlemi
    public List<PlayerDTO> getAllPlayers() {
        return playerRepository.findAllByOrderByNameAsc() // Tüm oyuncular
                .stream()
                .map(this::convertToPlayerDTO) // DTO dönüşümü PlayerStats'ı da içerecek şekilde güncellendi
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlayerDTO getPlayerById(Long id) {
        return playerRepository.findById(id)
                .map(this::convertToPlayerDTO) // DTO dönüşümü PlayerStats'ı da içerecek şekilde güncellendi
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + id));
    }

    @Transactional // Veri yazma işlemi
    public PlayerDTO addPlayer(PlayerCreateDTO createDTO) {
        // Aynı isimde oyuncu var mı kontrol et (büyük/küçük harf duyarsız)
        playerRepository.findByNameIgnoreCase(createDTO.getName()).ifPresent(p -> {
            throw new ResourceAlreadyExistsException("Player already exists with name: " + createDTO.getName());
        });

        Player player = new Player();
        player.setName(createDTO.getName());
        player.setIsActive(true); // Yeni eklenen oyuncu aktif olsun

        // YENİ: PlayerStats oluştur ve Player ile ilişkilendir
        PlayerStats stats = new PlayerStats();
        // stats.setWinCount(0); // PlayerStats entity'sinde default olarak 0 ayarlandıysa bu satırlara gerek yok
        // stats.setLoseCount(0); // PlayerStats entity'sinde default olarak 0 ayarlandıysa bu satırlara gerek yok
        
        // Player entity'sindeki setPlayerStats metodu, PlayerStats'a player'ı set etmeli (çift yönlü ilişki)
        player.setPlayerStats(stats); // Bu, stats.setPlayer(player) çağrısını da içermeli

        Player savedPlayer = playerRepository.save(player);
        // CascadeType.ALL sayesinde 'stats' da 'player' ile birlikte kaydedilecektir.
        // PlayerStats'ın player_id'si @MapsId sayesinde savedPlayer.getId() ile dolacaktır.

        return convertToPlayerDTO(savedPlayer);
    }

    @Transactional
    public void deletePlayer(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + id));

        // CascadeType.ALL ve orphanRemoval=true (Player entity'sindeki PlayerStats ilişkisinde)
        // sayesinde ilişkili PlayerStats kaydı da otomatik olarak silinecektir.
        playerRepository.delete(player);
    }

    // Entity -> DTO dönüşümü için helper method (PlayerStats bilgileri eklendi)
    private PlayerDTO convertToPlayerDTO(Player player) {
        PlayerStats stats = player.getPlayerStats(); // LAZY loading ise @Transactional context içinde olmalı

        Integer winCount = 0; // Varsayılan değerler
        Integer loseCount = 0; // Varsayılan değerler
        Integer drawCount = 0; // Varsayılan değer
        Double winLoseRatio = 0.0; // Varsayılan değer

        if (stats != null) {
            winCount = stats.getWinCount();
            loseCount = stats.getLoseCount();
            drawCount = stats.getDrawCount();

            if (loseCount > 0) {
                winLoseRatio = (double) winCount / loseCount;
            } else if (winCount > 0) {
                winLoseRatio = Double.POSITIVE_INFINITY; // Veya sadece winCount olarak göster, ya da özel bir değer
            } else {
                winLoseRatio = 0.0; // İkisi de sıfırsa oran sıfırdır.
            }
        } else {
             // Eğer bir oyuncunun stats kaydı yoksa (ki olmamalı), loglama yapılabilir.
             // System.err.println("Warning: PlayerStats not found for player ID: " + player.getId());
        }


        return new PlayerDTO(
                player.getId(),
                player.getName(),
                player.getCreatedAt(),
                player.getUpdatedAt(),
                player.getIsActive(),
                winCount,
                loseCount,
                drawCount,
                winLoseRatio
        );
    }

    @Transactional
    public void populateHistoricalPlayerStats() {
        System.out.println("Starting historical player stats population...");

        List<Player> allPlayers = playerRepository.findAll();
        for (Player player : allPlayers) {
            if (player.getId() == null) {
                System.err.println("Skipping player with null ID: " + player.getName());
                continue;
            }

            try {
                PlayerStats statsEntity;
                Optional<PlayerStats> optStats = playerStatsRepository.findById(player.getId());

                if (optStats.isPresent()) {
                    statsEntity = optStats.get();
                    System.out.println("Found existing PlayerStats for player: " + player.getName() + " with playerId: " + statsEntity.getPlayerId());

                    if (statsEntity.getPlayerId() == null) {
                        System.err.println("WARNING: Existing PlayerStats for player " + player.getName() + " (ID: " + player.getId() + ") has NULL playerId. Attempting to fix.");
                        statsEntity.setPlayerId(player.getId());
                    }

                    if (statsEntity.getPlayer() == null || !statsEntity.getPlayer().getId().equals(player.getId())) {
                        System.err.println("WARNING: Existing PlayerStats for player " + player.getName() + " has incorrect Player association. Attempting to fix.");
                        statsEntity.setPlayer(player);
                    }
                } else {
                    statsEntity = new PlayerStats();
                    System.out.println("Creating new PlayerStats for player: " + player.getName());
                    statsEntity.setPlayer(player);
                    statsEntity.setPlayerId(player.getId());
                }

                player.setPlayerStats(statsEntity);

                statsEntity.setWinCount(0);
                statsEntity.setLoseCount(0);
                statsEntity.setDrawCount(0);

                if (statsEntity.getPlayerId() == null) {
                    throw new IllegalStateException("PlayerStats ID is still null after setup for player " + player.getName());
                }

                if (statsEntity.getPlayer() == null || !statsEntity.getPlayer().getId().equals(player.getId())) {
                    throw new IllegalStateException("PlayerStats is not correctly associated with Player " + player.getName());
                }

                playerRepository.save(player);
                playerStatsRepository.save(statsEntity);

                System.out.println("Successfully saved PlayerStats for player: " + player.getName());
            } catch (Exception e) {
                System.err.println("Error processing player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                continue;
            }
        }
        System.out.println("Initialized/Reset stats for " + allPlayers.size() + " players.");

        List<Match> completedMatches = matchRepository.findAllWithScoresAndLineups();
        System.out.println("Found " + completedMatches.size() + " completed matches to process.");

        for (Match match : completedMatches) {
            try {
                MatchScore score = match.getMatchScore();
                if (score == null) {
                    System.err.println("Skipping match ID " + match.getId() + " due to null score.");
                    continue;
                }

                Integer teamAScore = score.getTeamAScore();
                Integer teamBScore = score.getTeamBScore();
                
                if (teamAScore == null || teamBScore == null) {
                    System.err.println("Skipping match ID " + match.getId() + " due to null team scores.");
                    continue;
                }

                Set<LineupPosition> positions = match.getLineupPositions();
                if (positions == null || positions.isEmpty()) {
                    System.err.println("No lineup positions found for match ID: " + match.getId() + ". Skipping player stats update.");
                    continue;
                }

                List<Player> teamAPlayers = positions.stream()
                        .filter(lp -> lp != null && "A".equalsIgnoreCase(lp.getTeamIdentifier()) && lp.getPlayer() != null)
                        .map(LineupPosition::getPlayer)
                        .collect(Collectors.toList());

                List<Player> teamBPlayers = positions.stream()
                        .filter(lp -> lp != null && "B".equalsIgnoreCase(lp.getTeamIdentifier()) && lp.getPlayer() != null)
                        .map(LineupPosition::getPlayer)
                        .collect(Collectors.toList());

                if (teamAPlayers.isEmpty() || teamBPlayers.isEmpty()) {
                    System.err.println("Skipping match ID " + match.getId() + " due to empty team rosters.");
                    continue;
                }

                if (teamAScore > teamBScore) {
                    applyWinToPlayers(teamAPlayers);
                    applyLoseToPlayers(teamBPlayers);
                } else if (teamBScore > teamAScore) {
                    applyWinToPlayers(teamBPlayers);
                    applyLoseToPlayers(teamAPlayers);
                } else {
                    // Beraberlik durumu
                    applyDrawToPlayers(teamAPlayers);
                    applyDrawToPlayers(teamBPlayers);
                }
            } catch (Exception e) {
                System.err.println("Error processing match ID " + match.getId() + ": " + e.getMessage());
                e.printStackTrace();
                continue;
            }
        }
        System.out.println("Finished historical player stats population.");
    }

    // Yardımcı metodlar (populateHistoricalPlayerStats içinde private olarak da tanımlanabilirler)
    private void applyWinToPlayers(List<Player> players) {
        for (Player player : players) {
            if (player == null || player.getId() == null) continue;
            playerStatsRepository.findById(player.getId()).ifPresent(stats -> {
                stats.setWinCount(stats.getWinCount() + 1);
                playerStatsRepository.save(stats);
            });
        }
    }

    private void applyLoseToPlayers(List<Player> players) {
        for (Player player : players) {
             if (player == null || player.getId() == null) continue;
            playerStatsRepository.findById(player.getId()).ifPresent(stats -> {
                stats.setLoseCount(stats.getLoseCount() + 1);
                playerStatsRepository.save(stats);
            });
        }
    }

    private void applyDrawToPlayers(List<Player> players) {
        for (Player player : players) {
            if (player == null || player.getId() == null) continue;
            playerStatsRepository.findById(player.getId()).ifPresent(stats -> {
                stats.setDrawCount(stats.getDrawCount() + 1);
                playerStatsRepository.save(stats);
            });
        }
    }
@Transactional(readOnly = true)
    public PlayerHistoryDTO getPlayerHistory(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        // Tüm maçları tarih sırasına göre çek
        List<Match> allMatches = matchRepository.findAllByOrderBySavedAtDesc();

        // Oyuncunun oynadığı maçların ID'lerini bir sete topla (hızlı erişim için)
        Set<Long> playedMatchIds = lineupPositionRepository.findByPlayerWithMatchDetails(playerId)
                .stream()
                .map(lp -> lp.getMatch().getId())
                .collect(Collectors.toSet());

        List<MatchHistoryItemDTO> matchHistory = new ArrayList<>();
        List<String> resultChars = new ArrayList<>();

        // Tüm maçlar üzerinden dön
        for (Match match : allMatches) {
            // Eğer oyuncu bu maçta oynamışsa
            if (playedMatchIds.contains(match.getId())) {
                // Oyuncunun bu maçtaki pozisyon bilgisini bul
                LineupPosition appearance = match.getLineupPositions().stream()
                        .filter(lp -> lp.getPlayer().getId().equals(playerId))
                        .findFirst()
                        .orElse(null);

                if (appearance == null) continue; // Beklenmedik bir durum, normalde olmamalı

                MatchScore score = match.getMatchScore();
                String playerTeam = appearance.getTeamIdentifier();
                String result;
                String resultChar;

                if (score != null && score.getTeamAScore() != null && score.getTeamBScore() != null) {
                    int teamAScore = score.getTeamAScore();
                    int teamBScore = score.getTeamBScore();

                    if (teamAScore == teamBScore) {
                        result = "DRAW";
                        resultChar = "B";
                    } else if (("A".equals(playerTeam) && teamAScore > teamBScore) || ("B".equals(playerTeam) && teamBScore > teamAScore)) {
                        result = "WIN";
                        resultChar = "G";
                    } else {
                        result = "LOSS";
                        resultChar = "M";
                    }
                } else {
                    result = "SCORE_PENDING";
                    resultChar = "?";
                }
                resultChars.add(resultChar);

                // Bu oynanmış maçı geçmiş listesine ekle
                matchHistory.add(MatchHistoryItemDTO.builder()
                        .matchId(match.getId())
                        .matchName(match.getMatchName())
                        .matchDate(match.getSavedAt())
                        .teamAScore(score != null ? score.getTeamAScore() : null)
                        .teamBScore(score != null ? score.getTeamBScore() : null)
                        .playerTeam(playerTeam)
                        .result(result)
                        .lineupA(matchService.convertToMatchDetailDTO(match).getLineupA())
                        .lineupB(matchService.convertToMatchDetailDTO(match).getLineupB())
                        .build());
            } else {
                // Oyuncu bu maçta oynamamışsa, sonuç serisine '-' ekle
                resultChars.add("-");
            }
        }

        PlayerStats stats = player.getPlayerStats();
        int winCount = stats != null ? stats.getWinCount() : 0;
        int drawCount = stats != null ? stats.getDrawCount() : 0;
        int loseCount = stats != null ? stats.getLoseCount() : 0;
        int totalGames = winCount + drawCount + loseCount;
        double winPercentage = (totalGames > 0) ? ((double) winCount / totalGames) * 100 : 0;

        return PlayerHistoryDTO.builder()
                .playerId(player.getId())
                .playerName(player.getName())
                .winCount(winCount)
                .drawCount(drawCount)
                .loseCount(loseCount)
                .totalGames(totalGames)
                .winPercentage(winPercentage)
                .resultSequence(String.join(" - ", resultChars))
                .matchHistory(matchHistory)
                .build();
    }
}