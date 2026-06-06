package com.healthtracker.water;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.DateRange;
import com.healthtracker.user.UserService;
import com.healthtracker.water.dto.WaterDay;
import com.healthtracker.water.dto.WaterEntry;
import com.healthtracker.water.dto.WaterRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WaterService {

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
