package com.gunes.cravings.service;


import com.gunes.cravings.dto.PlayerCreateDTO;
import com.gunes.cravings.dto.PlayerDTO;
import com.gunes.cravings.model.Player;
import com.gunes.cravings.exception.ResourceAlreadyExistsException;
import com.gunes.cravings.exception.ResourceNotFoundException;
import com.gunes.cravings.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Lombok: final field'lar için constructor inject eder
public class PlayerService {

    private final PlayerRepository playerRepository;

    // Tüm aktif oyuncuları getir (veya tümü, ihtiyaca göre)
    @Transactional(readOnly = true) // Veri okuma işlemi
    public List<PlayerDTO> getAllPlayers() {
        // return playerRepository.findByIsActiveTrueOrderByNameAsc() // Sadece aktifler
        return playerRepository.findAllByOrderByNameAsc() // Tüm oyuncular
                .stream()
                .map(this::convertToPlayerDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlayerDTO getPlayerById(Long id) {
        return playerRepository.findById(id)
                .map(this::convertToPlayerDTO)
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

        Player savedPlayer = playerRepository.save(player);
        return convertToPlayerDTO(savedPlayer);
    }

    @Transactional
    public void deletePlayer(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + id));

        // İlişkili veriler varsa ne yapılacağına karar verilmeli.
        // Şimdilik direkt silelim. Cascade ayarları Match tarafında pozisyonları siliyor.
        // Eğer oyuncu silindiğinde eski maçlardaki pozisyonları kalsın isteniyorsa,
        // oyuncuyu silmek yerine isActive=false yapmak daha iyi olabilir.
        // Veya LineupPosition'daki player ilişkisi nullable yapılabilir.

        // Seçenek 1: Direkt Silme (İlişkili LineupPosition'lar varsa hata verebilir - cascade yoksa)
         playerRepository.delete(player);

        // Seçenek 2: Pasif Hale Getirme (Daha güvenli olabilir)
        // player.setIsActive(false);
        // playerRepository.save(player);
    }


    // Entity -> DTO dönüşümü için helper method
    private PlayerDTO convertToPlayerDTO(Player player) {
        return new PlayerDTO(
                player.getId(),
                player.getName(),
                player.getCreatedAt(),
                player.getUpdatedAt(),
                player.getIsActive()
        );
    }
}