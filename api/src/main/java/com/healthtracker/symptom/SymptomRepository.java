package com.healthtracker.symptom;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SymptomRepository extends JpaRepository<Symptom, Long> {

    Optional<Symptom> findByIdAndUserId(Long id, Long userId);

    List<Symptom> findByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            Long userId, Instant start, Instant end);

    Optional<Symptom> findFirstByUserIdAndNameIgnoreCaseOrderByOccurredAtDesc(Long userId, String name);

    @Query("select s.name from Symptom s where s.userId = :userId group by s.name order by count(s) desc, s.name")
    List<String> findNamesByFrequency(Long userId);

    @Query("select t from Symptom s join s.tags t where s.userId = :userId group by t order by count(t) desc, t")
    List<String> findTagsByFrequency(Long userId);
}
