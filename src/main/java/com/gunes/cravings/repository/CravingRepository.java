package com.gunes.cravings.repository;

import com.gunes.cravings.model.Craving;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CravingRepository extends JpaRepository<Craving, Long> {
    List<Craving> findByIntensity(int intensity);
}