package com.healthtracker.activity.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Totals for one ISO week (Monday to Sunday). */
public record WeeklyActivity(
        LocalDate weekStart, int sessions, int activeMinutes, BigDecimal distanceKm, long steps, long caloriesKcal) {}
