package com.gunes.cravings.controller;

import com.gunes.cravings.dto.ReferralCodeGenerateRequestDTO; // New DTO
import com.gunes.cravings.dto.ReferralCodeResponseDTO;    // New DTO
import com.gunes.cravings.dto.UpdateMaxUsesRequestDTO;      // New DTO
import com.gunes.cravings.model.ReferralCode;
// import com.gunes.cravings.model.ReferralCode; // No longer returning entity directly
import com.gunes.cravings.service.PlayerService;
import com.gunes.cravings.service.ReferralCodeService;
import jakarta.validation.Valid; // For request body validation
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
// import java.util.stream.Collectors; // Not needed here anymore

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final PlayerService playerService;
    private final ReferralCodeService referralCodeService;

    @PostMapping("/populate-player-stats")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> populatePlayerStats() {
        // ... (existing code)
        try {
            playerService.populateHistoricalPlayerStats();
            return ResponseEntity.ok("Player stats population process initiated successfully.");
        } catch (Exception e) {
            System.err.println("Error during player stats population: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error during player stats population: " + e.getMessage());
        }
    }

    @PostMapping("/referral-codes/generate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ReferralCodeResponseDTO> generateReferralCode(
            @Valid @RequestBody(required = false) ReferralCodeGenerateRequestDTO requestDTO // Make body optional, handle null
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = authentication.getName();
        Integer maxUses = (requestDTO != null && requestDTO.getMaxUses() != null) ? requestDTO.getMaxUses() : 1; // Default to 1
        
        ReferralCode newCode = referralCodeService.generateReferralCode(adminEmail, maxUses);
        // Convert to DTO before sending response
        ReferralCodeResponseDTO responseDTO = ReferralCodeResponseDTO.builder()
                .id(newCode.getId())
                .code(newCode.getCode())
                .isActive(newCode.isActive())
                .createdAt(newCode.getCreatedAt())
                .createdByEmail(newCode.getCreatedBy() != null ? newCode.getCreatedBy().getEmail() : null)
                .maxUses(newCode.getMaxUses())
                .timesUsed(newCode.getTimesUsed() != null ? newCode.getTimesUsed() : 0) // Handle null safely
                .usedByEmail(newCode.getUsedBy() != null ? newCode.getUsedBy().getEmail() : null)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @GetMapping("/referral-codes")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ReferralCodeResponseDTO>> getAllReferralCodes() { // Returns DTO list
        List<ReferralCodeResponseDTO> codes = referralCodeService.getAllReferralCodesAsDTO();
        return ResponseEntity.ok(codes);
    }

    @PutMapping("/referral-codes/{codeId}/activate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ReferralCodeResponseDTO> activateReferralCode(@PathVariable Long codeId) { // Returns DTO
        ReferralCodeResponseDTO updatedCode = referralCodeService.toggleReferralCodeStatus(codeId, true);
        return ResponseEntity.ok(updatedCode);
    }

    @PutMapping("/referral-codes/{codeId}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ReferralCodeResponseDTO> deactivateReferralCode(@PathVariable Long codeId) { // Returns DTO
        ReferralCodeResponseDTO updatedCode = referralCodeService.toggleReferralCodeStatus(codeId, false);
        return ResponseEntity.ok(updatedCode);
    }

    @PutMapping("/referral-codes/{codeId}/max-uses")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ReferralCodeResponseDTO> updateReferralCodeMaxUses(
            @PathVariable Long codeId,
            @Valid @RequestBody UpdateMaxUsesRequestDTO requestDTO
    ) {
        ReferralCodeResponseDTO updatedCode = referralCodeService.updateMaxUses(codeId, requestDTO.getMaxUses());
        return ResponseEntity.ok(updatedCode);
    }
}