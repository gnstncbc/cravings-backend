package com.gunes.cravings.repository;

import com.gunes.cravings.model.Craving;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface CravingRepository extends JpaRepository<Craving, Long> {
    List<Craving> findByIntensity(int intensity);

    //startTime ve endTime arasındaki craving kayıtlarını getir
    List<Craving> findByStartTimeBetweenAndEndTimeBetween(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime startTime2, LocalDateTime endTime2);
}