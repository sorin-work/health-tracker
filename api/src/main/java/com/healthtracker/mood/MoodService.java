package com.healthtracker.mood;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.DateRange;
import com.healthtracker.common.PageResponse;
import com.healthtracker.mood.dto.MoodDay;
import com.healthtracker.mood.dto.MoodRequest;
import com.healthtracker.mood.dto.MoodResponse;
import com.healthtracker.user.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoodService {

    static final int MAX_RANGE_DAYS = 366;

    private final MoodRepository moods;
    private final UserService userService;
    private final Clock clock;

    public MoodService(MoodRepository moods, UserService userService, Clock clock) {
        this.moods = moods;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<MoodResponse> list(Long userId, LocalDate from, LocalDate to, int page, int size) {
        return PageResponse.of(entries(userId, from, to), page, size);
    }

    /** Check-ins in a date range, newest first. */
    @Transactional(readOnly = true)
    public List<MoodResponse> entries(Long userId, LocalDate from, LocalDate to) {
        return inRange(userId, DateRange.of(from, to, userService.zoneOf(userId))).stream()
                .map(MoodResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MoodDay> daily(Long userId, LocalDate from, LocalDate to) {
        ZoneId zone = userService.zoneOf(userId);
        LocalDate end = to != null ? to : LocalDate.now(clock.withZone(zone));
        LocalDate start = from != null ? from : end.minusDays(29);
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw ApiException.badRequest("Pick a range of at most " + MAX_RANGE_DAYS + " days");
        }
        return MoodStats.daily(inRange(userId, DateRange.of(start, end, zone)), zone);
    }

    @Transactional
    public MoodResponse create(Long userId, MoodRequest request) {
        MoodEntry entry = new MoodEntry(userId, clock.instant());
        apply(entry, request);
        return MoodResponse.from(moods.save(entry));
    }

    @Transactional
    public MoodResponse update(Long userId, Long id, MoodRequest request) {
        MoodEntry entry = find(userId, id);
        apply(entry, request);
        return MoodResponse.from(entry);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        moods.delete(find(userId, id));
    }

    List<MoodEntry> inRange(Long userId, DateRange range) {
        return moods.findByUserIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtDesc(
                userId, range.start(), range.end());
    }

    private MoodEntry find(Long userId, Long id) {
        return moods.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Mood entry"));
    }

    private void apply(MoodEntry entry, MoodRequest request) {
        if (request.recordedAt().isAfter(clock.instant().plusSeconds(600))) {
            throw ApiException.badRequest("Mood check-ins cannot be in the future");
        }
        entry.setMood(request.mood());
        entry.setEnergy(request.energy());
        entry.setRecordedAt(request.recordedAt());
        entry.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
    }
}
