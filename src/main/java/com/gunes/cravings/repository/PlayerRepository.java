package com.gunes.cravings.repository;

import com.gunes.cravings.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    // İsme göre oyuncu bul (yeni oyuncu eklerken kontrol için)
    Optional<Player> findByNameIgnoreCase(String name);

    // Sadece aktif oyuncuları listelemek için (opsiyonel)
    List<Player> findByIsActiveTrueOrderByNameAsc();

    // Tüm oyuncuları isim sırasına göre listelemek için
    List<Player> findAllByOrderByNameAsc();
}
