package com.healthtracker.mood;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoodRepository extends JpaRepository<MoodEntry, Long> {

    Optional<MoodEntry> findByIdAndUserId(Long id, Long userId);

    List<MoodEntry> findByUserIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
            Long userId, Instant start, Instant end);
}
