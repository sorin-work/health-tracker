package com.healthtracker.medication;

/**
 * Counts of dose outcomes. {@code percent} is taken / (taken + skipped + missed); pending doses are
 * left out because they can still be taken. Null when nothing was due yet.
 */
public record AdherenceStats(int scheduled, int taken, int skipped, int missed, int pending, Double percent) {}
