package com.healthtracker.medication.dto;

import com.healthtracker.medication.DoseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record RecordDoseRequest(@NotNull Instant scheduledAt, @NotNull DoseStatus status, @Size(max = 500) String note) {}
