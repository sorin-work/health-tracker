package com.healthtracker.journal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JournalRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);

    /** Newest first; {@code query} matches title or body, ignoring case. Null means no text filter. */
    @Query("""
            select j from JournalEntry j
            where j.userId = :userId and j.entryDate between :from and :to
              and (:query is null
                   or lower(j.body) like lower(concat('%', :query, '%'))
                   or lower(j.title) like lower(concat('%', :query, '%')))
            order by j.entryDate desc, j.createdAt desc
            """)
    List<JournalEntry> search(Long userId, LocalDate from, LocalDate to, String query);
}
