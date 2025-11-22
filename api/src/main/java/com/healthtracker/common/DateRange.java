package com.healthtracker.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * An inclusive range of calendar days in the user's time zone, plus the matching instants.
 * {@code end} is exclusive (start of the day after {@code to}).
 */
public record DateRange(LocalDate from, LocalDate to, Instant start, Instant end) {

    private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
    private static final LocalDate LATEST = LocalDate.of(2999, 12, 31);

    public static DateRange of(LocalDate from, LocalDate to, ZoneId zone) {
        LocalDate f = from == null ? EARLIEST : from;
        LocalDate t = to == null ? LATEST : to;
        if (t.isBefore(f)) {
            throw ApiException.badRequest("'to' must not be before 'from'");
        }
        return new DateRange(f, t, f.atStartOfDay(zone).toInstant(), t.plusDays(1).atStartOfDay(zone).toInstant());
    }

    public boolean contains(LocalDate day) {
        return !day.isBefore(from) && !day.isAfter(to);
    }
}
