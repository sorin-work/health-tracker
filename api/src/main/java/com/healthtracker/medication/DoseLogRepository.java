package com.healthtracker.medication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoseLogRepository extends JpaRepository<DoseLog, Long> {

    List<DoseLog> findByUserIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThan(
            Long userId, Instant start, Instant end);

    Optional<DoseLog> findByMedicationIdAndScheduledAt(Long medicationId, Instant scheduledAt);

    Optional<DoseLog> findByIdAndUserId(Long id, Long userId);
}
