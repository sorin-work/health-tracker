package com.healthtracker.journal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JournalRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);

    /** Entries between two days, newest first. */
    List<JournalEntry> findByUserIdAndEntryDateBetweenOrderByEntryDateDescCreatedAtDesc(
            Long userId, LocalDate from, LocalDate to);

    /**
     * Newest first; {@code query} matches title or body, ignoring case. It must not be null: Postgres
     * cannot type a null parameter inside lower(), so callers without a search term use the method
     * above. The query is a LIKE pattern escaped with a backslash; use {@link #likePattern} to build it.
     */
    @Query("""
            select j from JournalEntry j
            where j.userId = :userId and j.entryDate between :from and :to
              and (lower(j.body) like lower(concat('%', :query, '%')) escape '\\'
                   or lower(j.title) like lower(concat('%', :query, '%')) escape '\\')
            order by j.entryDate desc, j.createdAt desc
            """)
    List<JournalEntry> search(Long userId, LocalDate from, LocalDate to, String query);

    /** Escapes LIKE wildcards so "100%" or "a_b" are searched for literally. */
    static String likePattern(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
