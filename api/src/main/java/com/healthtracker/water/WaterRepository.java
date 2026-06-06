package com.healthtracker.water;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterRepository extends JpaRepository<WaterIntake, Long> {

    Optional<WaterIntake> findByIdAndUserId(Long id, Long userId);

    List<WaterIntake> findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtAsc(
            Long userId, Instant start, Instant end);
}
