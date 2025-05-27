package com.gunes.cravings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralCodeResponseDTO {
    private Long id;
    private String code;
    private boolean isActive;
    private LocalDateTime createdAt;
    private String createdByEmail;
    private String usedByEmail; // Will be null if not used or for multi-use view
    private LocalDateTime usedAt;
    private Integer maxUses;
    private Integer timesUsed;
}