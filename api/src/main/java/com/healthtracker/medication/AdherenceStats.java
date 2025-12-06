package com.healthtracker.medication;

/** Counts of dose outcomes. {@code percent} is taken / scheduled, or null when nothing was scheduled. */
public record AdherenceStats(int scheduled, int taken, int skipped, int missed, int pending, Double percent) {}
