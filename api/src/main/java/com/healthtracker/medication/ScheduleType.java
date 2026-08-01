package com.healthtracker.medication;

public enum ScheduleType {
    /** Every day at the medication's times. */
    DAILY,
    /** Every N days, counted from the start date (2 = every other day). */
    EVERY_N_DAYS,
    /** On chosen days of the week. */
    WEEKLY
}
