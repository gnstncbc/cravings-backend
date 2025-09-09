package com.gunes.cravings.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gunes.cravings.dto.BorcCreateRequestDTO;
import com.gunes.cravings.model.Borc;
import com.gunes.cravings.service.BorcService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/para/borc")
public class BorcController {
    private final BorcService borcService;

    @PostMapping("create-borc")
    public ResponseEntity<BorcCreateRequestDTO> createBorc(@RequestBody BorcCreateRequestDTO borc) {
        borcService.saveBorc(borc);
        return ResponseEntity.ok(borc);
    }

    @GetMapping("get-all-borc")
    public ResponseEntity<List<Borc>> getAllBorc(@RequestParam(required = false) String filter) {
        List<Borc> borcs = borcService.getAllBorc();
        return ResponseEntity.ok(borcs);
    }

    @DeleteMapping("delete-borc")
    public ResponseEntity<String> deleteBorc(@RequestParam Long id) {
        // Borc silme işlemi burada yapılacak (henüz implement edilmedi)
        borcService.deleteBorc(id);
        return ResponseEntity.ok("Borc başarıyla silindi.");
    }
    
    @GetMapping("total-borc-amount")
    public ResponseEntity<String> getTotalBorcAmount(@RequestParam(required = false) String filter) {
        return ResponseEntity.ok("Toplam borç miktarı: "+ borcService.getAllBorc().stream().filter(b -> !b.getIsPaid()).mapToLong(Borc::getBorcAmount).sum());
    }

    @PutMapping("set-paid/{id}")
    public ResponseEntity<String> setBorcPaid(@PathVariable String id) {
        return ResponseEntity.ok(borcService.setBorcPaid(id));
    }
    

}
