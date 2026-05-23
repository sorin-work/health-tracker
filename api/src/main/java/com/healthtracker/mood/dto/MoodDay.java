package com.healthtracker.mood.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Average mood (and energy, when logged) for one calendar day in the user's zone. */
public record MoodDay(LocalDate date, BigDecimal mood, BigDecimal energy, int count) {}
