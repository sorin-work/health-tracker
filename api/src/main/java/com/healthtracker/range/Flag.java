package com.healthtracker.range;

/** Where a reading sits relative to its reference range. */
public enum Flag {
    CRITICAL_LOW(2),
    LOW(1),
    NORMAL(0),
    HIGH(1),
    CRITICAL_HIGH(2);

    private final int severity;

    Flag(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }

    public boolean outOfRange() {
        return this != NORMAL;
    }

    /** The more severe of two flags; on a tie the first one wins. */
    public static Flag worst(Flag a, Flag b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return b.severity > a.severity ? b : a;
    }
}
