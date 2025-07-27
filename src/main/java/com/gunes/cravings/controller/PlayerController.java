package com.gunes.cravings.controller;


import com.gunes.cravings.dto.PlayerCreateDTO;
import com.gunes.cravings.dto.PlayerDTO;
import com.gunes.cravings.dto.PlayerHistoryDTO;
import com.gunes.cravings.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/players") // Temel path
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Global CORS ayarı
public class PlayerController {

    private final PlayerService playerService;
    
    @GetMapping
    public ResponseEntity<List<PlayerDTO>> getAllPlayers() {
        List<PlayerDTO> players = playerService.getAllPlayers();
        return ResponseEntity.ok(players);
    }

     @GetMapping("/{id}")
    public ResponseEntity<PlayerDTO> getPlayerById(@PathVariable Long id) {
        PlayerDTO player = playerService.getPlayerById(id);
        return ResponseEntity.ok(player);
    }

    @PostMapping
    public ResponseEntity<PlayerDTO> addPlayer(@Valid @RequestBody PlayerCreateDTO createDTO) {
        PlayerDTO newPlayer = playerService.addPlayer(createDTO);
        // 201 Created status kodu ve yeni kaynağın bilgisiyle dönmek iyi pratiktir
        return ResponseEntity.status(HttpStatus.CREATED).body(newPlayer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/{playerId}/history")
    public ResponseEntity<PlayerHistoryDTO> getPlayerHistory(@PathVariable Long playerId) {
        PlayerHistoryDTO history = playerService.getPlayerHistory(playerId);
        return ResponseEntity.ok(history);
    }
}
