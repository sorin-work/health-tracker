package com.healthtracker.medication;

import java.time.Instant;
import java.time.LocalDate;

/** A dose slot from a medication's schedule, joined with whatever was logged for it. */
public record ScheduledDose(Medication medication, LocalDate day, Instant scheduledAt, DoseLog log, DoseStatus status) {}
