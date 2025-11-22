package com.healthtracker.vital;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitalReadingRepository extends JpaRepository<VitalReading, Long> {

    Optional<VitalReading> findByIdAndUserId(Long id, Long userId);

    List<VitalReading> findByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
            Long userId, Instant start, Instant end);

    List<VitalReading> findByUserIdAndTypeAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
            Long userId, VitalType type, Instant start, Instant end);

    Optional<VitalReading> findFirstByUserIdAndTypeOrderByMeasuredAtDesc(Long userId, VitalType type);

    long countByUserIdAndType(Long userId, VitalType type);

    default List<VitalReading> findInRange(Long userId, VitalType type, Instant start, Instant end) {
        return type == null
                ? findByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(userId, start, end)
                : findByUserIdAndTypeAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                        userId, type, start, end);
    }
}
