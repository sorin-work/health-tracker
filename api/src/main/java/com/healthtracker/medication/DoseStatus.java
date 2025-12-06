package com.healthtracker.medication;

public enum DoseStatus {
    /** Logged as taken. */
    TAKEN,
    /** Deliberately skipped (and logged as such). */
    SKIPPED,
    /** Nothing logged and the day is over. */
    MISSED,
    /** Nothing logged yet, the day is not over. */
    PENDING
}
