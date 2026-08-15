package com.healthtracker.medication;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Works out how long a medication's supply lasts at its current schedule. */
public final class SupplyEstimator {

    private SupplyEstimator() {}

    /** How many days one schedule cycle covers: 1 for daily, N for every N days, 7 for weekly. */
    private static int cycleDays(Medication m) {
        return switch (m.getScheduleType()) {
            case DAILY -> 1;
            case EVERY_N_DAYS -> m.getIntervalDays() == null ? 1 : m.getIntervalDays();
            case WEEKLY -> 7;
        };
    }

    /** Doses in one cycle: two times a day on Mondays and Thursdays is four doses a week. */
    private static int dosesPerCycle(Medication m) {
        if (m.isAsNeeded()) {
            return 0;
        }
        int times = m.getTimes().size();
        return m.getScheduleType() == ScheduleType.WEEKLY ? times * m.getDaysOfWeek().size() : times;
    }

    /**
     * Whole days the supply lasts, or null when it is not tracked or the medication has no schedule
     * (as-needed medications can run out at any time).
     */
    public static Integer daysLeft(Medication m) {
        int doses = dosesPerCycle(m);
        if (m.getSupply() == null || doses == 0) {
            return null;
        }
        // supply / (doses per day * quantity), kept as one division so 4 weekly doses are exactly 28 days.
        BigDecimal unitsPerCycle = m.getDoseQuantity().multiply(BigDecimal.valueOf(doses));
        return m.getSupply()
                .multiply(BigDecimal.valueOf(cycleDays(m)))
                .divide(unitsPerCycle, 0, RoundingMode.FLOOR)
                .intValue();
    }

    /** True when an active, tracked medication runs out within its warning window (or already has). */
    public static boolean isLow(Medication m, LocalDate today) {
        if (!m.isActive() || (m.getEndDate() != null && m.getEndDate().isBefore(today))) {
            return false;
        }
        if (m.isAsNeeded()) {
            return m.getSupply() != null && m.getSupply().compareTo(m.getDoseQuantity()) < 0;
        }
        Integer days = daysLeft(m);
        return days != null && days <= m.getRefillWarningDays();
    }
}
