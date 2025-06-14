package com.gunes.cravings.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gunes.cravings.model.Para;

public interface ParaRepository extends JpaRepository<Para, Long> {
    // Para ile ilgili özel sorgular eklenebilir.
    // Örneğin, kullanıcı ID'sine göre bakiye bulmak için:
    // findByUserId(Long userId) -> Zaten JpaRepository.findById(userId) bunu yapar.
    
}
