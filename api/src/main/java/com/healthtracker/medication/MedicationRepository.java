package com.healthtracker.medication;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findByUserIdOrderByNameAsc(Long userId);

    Optional<Medication> findByIdAndUserId(Long id, Long userId);
}
