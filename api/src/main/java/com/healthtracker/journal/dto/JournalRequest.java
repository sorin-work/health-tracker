package com.healthtracker.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record JournalRequest(
        @NotNull LocalDate entryDate, @Size(max = 120) String title, @NotBlank @Size(max = 5000) String body) {}
