package com.healthtracker.activity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Optional<Activity> findByIdAndUserId(Long id, Long userId);

    List<Activity> findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
            Long userId, Instant start, Instant end);
}
