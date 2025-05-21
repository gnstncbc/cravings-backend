package com.gunes.cravings.controller;

import com.gunes.cravings.service.PlayerService; // PlayerService'e ekleyeceğimiz metodu çağıracağız
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Gerekirse spesifik domainlere kısıtlayın
public class AdminController {

    private final PlayerService playerService; // Ya da yeni bir DataMigrationService

    @PostMapping("/populate-player-stats")
    @PreAuthorize("hasAuthority('ADMIN')") // Sadece ADMIN rolüne sahip kullanıcılar erişebilir
    public ResponseEntity<String> populatePlayerStats() {
        try {
            playerService.populateHistoricalPlayerStats();
            return ResponseEntity.ok("Player stats population process initiated successfully.");
        } catch (Exception e) {
            // Detaylı hata loglaması yapılması önerilir
            System.err.println("Error during player stats population: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error during player stats population: " + e.getMessage());
        }
    }
}