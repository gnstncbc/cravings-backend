package com.gunes.cravings.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gunes.cravings.dto.ParaRequestDTO;
import com.gunes.cravings.dto.ParaResponseDTO;
import com.gunes.cravings.dto.ParaStartRequestDTO;
import com.gunes.cravings.model.Para;
import com.gunes.cravings.service.ParaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/para")
public class ParaController {
    private final ParaService paraService;
    private BigDecimal remainingMoney = BigDecimal.ZERO;
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Para API çalışıyor!");
    }

    @PostMapping("/remaining-money")
    public ResponseEntity<ParaResponseDTO> remainingMoney(
        @RequestBody ParaRequestDTO paraRequestDTO
    ) {
        paraService.setcreditCardRemainingLimit(paraRequestDTO.getCreditCardRemainingLimit());
        remainingMoney = paraService.calculateRemainingMoney();
        ParaResponseDTO paraResponseDTO = new ParaResponseDTO();
        paraResponseDTO.setRemainingMoney(remainingMoney);
        return ResponseEntity.ok(paraResponseDTO);
    }

    @PostMapping("/init")
    public ResponseEntity<String> initParaTable(@RequestBody ParaStartRequestDTO paraInitRequestDTO) {
        paraService.initParaTable(paraInitRequestDTO);
        return ResponseEntity.ok("Para tablosu başarıyla başlatıldı.");
    }
    
}
