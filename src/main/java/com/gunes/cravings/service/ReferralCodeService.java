package com.gunes.cravings.service;

import com.gunes.cravings.dto.ReferralCodeResponseDTO; // Import new DTO
import com.gunes.cravings.exception.InvalidReferralCodeException;
import com.gunes.cravings.model.ReferralCode;
import com.gunes.cravings.model.User;
import com.gunes.cravings.repository.ReferralCodeRepository;
import com.gunes.cravings.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors; // For DTO mapping

@Service
@RequiredArgsConstructor
public class ReferralCodeService {

    private final ReferralCodeRepository referralCodeRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReferralCode generateReferralCode(String adminEmail, Integer maxUses) { // Added maxUses
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found: " + adminEmail));

        String codeValue;
        do {
            codeValue = UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 10); // Example: 10-char
        } while (referralCodeRepository.findByCode(codeValue).isPresent());

        ReferralCode referralCode = ReferralCode.builder()
                .code(codeValue)
                .createdBy(admin)
                .isActive(true)
                .maxUses(maxUses != null && maxUses > 0 ? maxUses : 1) // Ensure maxUses is at least 1
                .timesUsed(0)
                .build();
        return referralCodeRepository.save(referralCode);
    }

    @Transactional(readOnly = true)
    public List<ReferralCodeResponseDTO> getAllReferralCodesAsDTO() { // Changed to return DTO
        return referralCodeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ReferralCode findByCodeAndMarkAsUsed(String codeValue, User newUser) {
        ReferralCode referralCode = referralCodeRepository.findByCode(codeValue)
                .orElseThrow(() -> new InvalidReferralCodeException("Geçersiz referans kodu."));

        if (!referralCode.isActive()) {
            throw new InvalidReferralCodeException("Referans kodu aktif değil.");
        }
        // Check if the code has reached its max uses
        if (referralCode.getMaxUses() != null && referralCode.getTimesUsed() != null
            && referralCode.getTimesUsed() >= referralCode.getMaxUses()) {
            throw new InvalidReferralCodeException("Referans kodu maksimum kullanım limitine ulaştı.");
        }
        // If it's a single-use code strictly (maxUses = 1), usedBy should still be null.
        // For multi-use codes, this specific `usedBy` field might become less relevant or represent the *last* user.
        // For simplicity with current `usedBy` being OneToOne:
        if (referralCode.getMaxUses() == 1 && referralCode.getUsedBy() != null) {
            throw new InvalidReferralCodeException("Bu tek kullanımlık referans kodu daha önce kullanılmış.");
        }


        referralCode.setTimesUsed(
            (referralCode.getTimesUsed() == null ? 0 : referralCode.getTimesUsed()) + 1
        );
        
        // Only set usedBy if it's the first use and intended to be tracked that way
        // For multiple uses, you might have a separate UserReferralUsageLink table
        if (referralCode.getMaxUses() != null && referralCode.getMaxUses() == 1
            && referralCode.getTimesUsed() != null && referralCode.getTimesUsed() == 1) {
            referralCode.setUsedBy(newUser);
            referralCode.setUsedAt(LocalDateTime.now());
        }
        // If maxUses > 1, usedBy might represent the *last* user or stay null if you don't track individual users per code use here.
        // The current model has usedBy as OneToOne, so it implies a single user can "claim" the code.
        // If a code with maxUses > 1 is used, usedBy will be set to the first user.

        return referralCodeRepository.save(referralCode);
    }
    
    @Transactional
    public ReferralCodeResponseDTO toggleReferralCodeStatus(Long codeId, boolean isActive) { // Changed to return DTO
        ReferralCode referralCode = referralCodeRepository.findById(codeId)
                .orElseThrow(() -> new InvalidReferralCodeException("Referans kodu bulunamadı."));
        referralCode.setActive(isActive);
        return convertToDTO(referralCodeRepository.save(referralCode));
    }

    @Transactional
    public ReferralCodeResponseDTO updateMaxUses(Long codeId, Integer newMaxUses) { // Changed to return DTO
        ReferralCode referralCode = referralCodeRepository.findById(codeId)
                .orElseThrow(() -> new InvalidReferralCodeException("Referans kodu bulunamadı."));

        if (newMaxUses != null && referralCode.getTimesUsed() != null && newMaxUses < referralCode.getTimesUsed()) {
            throw new InvalidReferralCodeException("Maksimum kullanım sayısı, mevcut kullanım sayısından (" + referralCode.getTimesUsed() + ") az olamaz.");
        }
        referralCode.setMaxUses(newMaxUses);
        return convertToDTO(referralCodeRepository.save(referralCode));
    }

    // Helper to convert ReferralCode entity to ReferralCodeResponseDTO
    private ReferralCodeResponseDTO convertToDTO(ReferralCode code) {
        return ReferralCodeResponseDTO.builder()
            .id(code.getId())
            .code(code.getCode())
            .isActive(code.isActive())
            .createdAt(code.getCreatedAt())
            .createdByEmail(code.getCreatedBy() != null ? code.getCreatedBy().getEmail() : "N/A")
            .usedByEmail(code.getUsedBy() != null ? code.getUsedBy().getEmail() : null)
            .usedAt(code.getUsedAt())
            .maxUses(code.getMaxUses() != null ? code.getMaxUses() : 0)
            .timesUsed(code.getTimesUsed() != null ? code.getTimesUsed() : 0)
            .build();
    }
}