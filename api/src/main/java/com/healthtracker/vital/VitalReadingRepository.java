package com.healthtracker.vital;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitalReadingRepository extends JpaRepository<VitalReading, Long> {

    Optional<VitalReading> findByIdAndUserId(Long id, Long userId);

    List<VitalReading> findByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
            Long userId, Instant start, Instant end);

    List<VitalReading> findByUserIdAndTypeAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
            Long userId, VitalType type, Instant start, Instant end);

    Page<VitalReading> findByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
            Long userId, Instant start, Instant end, Pageable pageable);

    Page<VitalReading> findByUserIdAndTypeAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
            Long userId, VitalType type, Instant start, Instant end, Pageable pageable);

    Optional<VitalReading> findFirstByUserIdAndTypeOrderByMeasuredAtDesc(Long userId, VitalType type);

    long countByUserIdAndType(Long userId, VitalType type);

    default List<VitalReading> findInRange(Long userId, VitalType type, Instant start, Instant end) {
        return type == null
                ? findByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(userId, start, end)
                : findByUserIdAndTypeAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                        userId, type, start, end);
    }

    /** One page of readings, newest first, counted and sliced by the database. */
    default Page<VitalReading> pageInRange(Long userId, VitalType type, Instant start, Instant end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "measuredAt").and(Sort.by("id")));
        return type == null
                ? findByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(userId, start, end, pageable)
                : findByUserIdAndTypeAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                        userId, type, start, end, pageable);
    }
}
