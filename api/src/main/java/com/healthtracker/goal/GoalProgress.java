package com.healthtracker.goal;

import java.math.BigDecimal;

/**
 * @param percent 0-100
 * @param onTrack null when there is no target date (or the goal is already achieved)
 */
public record GoalProgress(BigDecimal currentValue, double percent, boolean achieved, Boolean onTrack) {}
