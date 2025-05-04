package com.gunes.cravings.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gunes.cravings.dto.LineupPositionInputDTO;


@RestController
@RequestMapping("/api/test") 
public class TestController {
    @PostMapping("/dto-check")
    public ResponseEntity<LineupPositionInputDTO> testDtoDeserialization(@RequestBody LineupPositionInputDTO dto) {
        System.out.println("--- DTO TEST BAŞLANGIÇ ---");
        System.out.println("Alınan DTO: " + dto); // Gelen DTO'yu logla
        if (dto != null) {
            System.out.println("dto.xPercent: " + dto.getXPercent());
            System.out.println("dto.yPercent: " + dto.getYPercent());
        } else {
            System.out.println("DTO null geldi!");
        }
        System.out.println("--- DTO TEST BİTİŞ ---");
        // Basitçe geleni geri dönelim veya sadece OK dönelim
        return ResponseEntity.ok(dto);
    }
}
