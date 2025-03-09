package com.gunes.cravings.controller;

import com.gunes.cravings.model.Craving;
import com.gunes.cravings.repository.CravingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

import java.util.List;

@RestController
@RequestMapping("/cravings")
public class CravingController {

    @Autowired
    private CravingRepository cravingRepository;

    // Tüm kayıtları getir
    @GetMapping
    public List<Craving> getAllCravings() {
        return cravingRepository.findAll();
    }

    // Yeni bir sigara isteği kaydı ekle
    @PostMapping("/save")
    public Craving saveCraving(@RequestBody Craving craving) {
        return cravingRepository.save(craving);
    }

    // Belirli bir craving kaydını sil
    @DeleteMapping("/{id}")
    public void deleteCraving(@PathVariable Long id) {
        cravingRepository.deleteById(id);
    }

    @DeleteMapping("/deleteAll")
    public void deleteAllCravings() {
        cravingRepository.deleteAll();
    }

    @GetMapping("/intensity/{intensity}")
    public List<Craving> getCravingsByIntensity(@PathVariable int intensity) {
        return cravingRepository.findByIntensity(intensity);
    }

    @GetMapping("/time/{startTime}/{endTime}/{startTime2}/{endTime2}")
    public List<Craving> getCravingsByTime(@PathVariable String startTime, @PathVariable String endTime, @PathVariable String startTime2, @PathVariable String endTime2) {
        return cravingRepository.findByStartTimeBetweenAndEndTimeBetween(LocalDateTime.parse(startTime), LocalDateTime.parse(endTime), LocalDateTime.parse(startTime2), LocalDateTime.parse(endTime2));
    }
}