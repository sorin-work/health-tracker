package com.healthtracker.range;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomRangeRepository extends JpaRepository<CustomRange, Long> {

    List<CustomRange> findByUserId(Long userId);

    Optional<CustomRange> findByUserIdAndMetric(Long userId, RangeMetric metric);
}
