package com.healthtracker.water.dto;

import java.time.LocalDate;
import java.util.List;

/** Everything drunk on one calendar day in the user's zone. */
public record WaterDay(LocalDate date, int totalMl, List<WaterEntry> entries) {}
