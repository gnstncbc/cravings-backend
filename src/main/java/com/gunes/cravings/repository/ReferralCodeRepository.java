package com.gunes.cravings.repository;

import com.gunes.cravings.model.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralCodeRepository extends JpaRepository<ReferralCode, Long> {
    Optional<ReferralCode> findByCode(String code);
    Optional<ReferralCode> findByCodeAndIsActiveTrue(String code);
    List<ReferralCode> findAllByOrderByCreatedAtDesc();
}