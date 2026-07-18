package com.healthtracker.journal.dto;

import com.healthtracker.journal.JournalEntry;
import java.time.Instant;
import java.time.LocalDate;

public record JournalResponse(
        Long id, LocalDate entryDate, String title, String body, Instant createdAt, Instant updatedAt) {

    public static JournalResponse from(JournalEntry j) {
        return new JournalResponse(
                j.getId(), j.getEntryDate(), j.getTitle(), j.getBody(), j.getCreatedAt(), j.getUpdatedAt());
    }
}
