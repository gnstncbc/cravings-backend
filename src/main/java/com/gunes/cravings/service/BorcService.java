package com.gunes.cravings.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gunes.cravings.dto.BorcCreateRequestDTO;
import com.gunes.cravings.repository.BorcRepository;
import com.gunes.cravings.model.Borc;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class BorcService {
    private final BorcRepository borcRepository;

    public void saveBorc(BorcCreateRequestDTO borcCreateRequestDTO) {
        // Yeni bir Borc nesnesi oluştur ve veritabanına kaydet
        var borc = new Borc();
        borc.setIsPaid(borcCreateRequestDTO.getIsPaid());
        borc.setBorcAmount(borcCreateRequestDTO.getBorcAmount());
        borc.setBorcName(borcCreateRequestDTO.getBorcName());
        // Borc nesnesini kaydet
        borcRepository.save(borc);

    }

    public List<Borc> getAllBorc() {
        return borcRepository.findAll();
    }

    public void deleteBorc(Long id) {
        borcRepository.deleteById(id);
    }

    public String setBorcPaid(String id) {
        var borc = borcRepository.findById(Long.valueOf(id)).orElseThrow();
        borc.setIsPaid(true);
        borcRepository.save(borc);
        return "Borc ödendi olarak işaretlendi.";
    }

}
