package com.healthtracker.water;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.DateRange;
import com.healthtracker.user.UserService;
import com.healthtracker.water.dto.WaterDay;
import com.healthtracker.water.dto.WaterEntry;
import com.healthtracker.water.dto.WaterRequest;
import com.healthtracker.water.dto.WaterTotal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WaterService {

    static final int MAX_RANGE_DAYS = 366;

    private final WaterRepository water;
    private final UserService userService;
    private final Clock clock;

    public WaterService(WaterRepository water, UserService userService, Clock clock) {
        this.water = water;
        this.userService = userService;
        this.clock = clock;
    }

    /** One day's entries and total; today when no date is given. */
    @Transactional(readOnly = true)
    public WaterDay day(Long userId, LocalDate date) {
        ZoneId zone = userService.zoneOf(userId);
        LocalDate day = date != null ? date : LocalDate.now(clock.withZone(zone));
        List<WaterEntry> entries = inRange(userId, DateRange.of(day, day, zone)).stream()
                .map(WaterEntry::from)
                .toList();
        return new WaterDay(day, entries.stream().mapToInt(WaterEntry::amountMl).sum(), entries);
    }

    /** Total per day between two dates, with zero for days without entries so charts have no gaps. */
    @Transactional(readOnly = true)
    public List<WaterTotal> daily(Long userId, LocalDate from, LocalDate to) {
        ZoneId zone = userService.zoneOf(userId);
        LocalDate end = to != null ? to : LocalDate.now(clock.withZone(zone));
        LocalDate start = from != null ? from : end.minusDays(29);
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw ApiException.badRequest("Pick a range of at most " + MAX_RANGE_DAYS + " days");
        }
        Map<LocalDate, Integer> totals = new HashMap<>();
        for (WaterIntake w : inRange(userId, DateRange.of(start, end, zone))) {
            totals.merge(LocalDate.ofInstant(w.getLoggedAt(), zone), w.getAmountMl(), Integer::sum);
        }
        List<WaterTotal> days = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            days.add(new WaterTotal(day, totals.getOrDefault(day, 0)));
        }
        return days;
    }

    /** Entries in a date range, oldest first. */
    @Transactional(readOnly = true)
    public List<WaterEntry> entries(Long userId, LocalDate from, LocalDate to) {
        return inRange(userId, DateRange.of(from, to, userService.zoneOf(userId))).stream()
                .map(WaterEntry::from)
                .toList();
    }

    @Transactional
    public WaterEntry add(Long userId, WaterRequest request) {
        Instant now = clock.instant();
        Instant at = request.loggedAt() != null ? request.loggedAt() : now;
        if (at.isAfter(now.plusSeconds(600))) {
            throw ApiException.badRequest("Water cannot be logged in the future");
        }
        return WaterEntry.from(water.save(new WaterIntake(userId, request.amountMl(), at, now)));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        water.delete(water.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Water entry")));
    }

    List<WaterIntake> inRange(Long userId, DateRange range) {
        return water.findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtAsc(
                userId, range.start(), range.end());
    }
}
