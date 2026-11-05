package com.healthtracker.journal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JournalRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);

    /**
     * Newest first; {@code query} matches title or body, ignoring case. Null means no text filter.
     * The query is a LIKE pattern escaped with a backslash; use {@link #likePattern} to build it.
     */
    @Query("""
            select j from JournalEntry j
            where j.userId = :userId and j.entryDate between :from and :to
              and (:query is null
                   or lower(j.body) like lower(concat('%', :query, '%')) escape '\\'
                   or lower(j.title) like lower(concat('%', :query, '%')) escape '\\')
            order by j.entryDate desc, j.createdAt desc
            """)
    List<JournalEntry> search(Long userId, LocalDate from, LocalDate to, String query);

    /** Escapes LIKE wildcards so "100%" or "a_b" are searched for literally. */
    static String likePattern(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
