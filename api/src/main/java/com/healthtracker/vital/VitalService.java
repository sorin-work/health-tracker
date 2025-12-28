package com.healthtracker.vital;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.DateRange;
import com.healthtracker.common.PageResponse;
import com.healthtracker.range.Flag;
import com.healthtracker.range.RangeFlagger;
import com.healthtracker.range.RangeService;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.dto.TrendPoint;
import com.healthtracker.vital.dto.VitalReadingRequest;
import com.healthtracker.vital.dto.VitalReadingResponse;
import com.healthtracker.vital.dto.VitalSummary;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VitalService {

    private final VitalReadingRepository readings;
    private final RangeService rangeService;
    private final UserService userService;
    private final Clock clock;

    public VitalService(
            VitalReadingRepository readings, RangeService rangeService, UserService userService, Clock clock) {
        this.readings = readings;
        this.rangeService = rangeService;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public VitalReadingResponse create(Long userId, VitalReadingRequest request) {
        VitalValidator.validate(request, clock.instant());
        VitalReading reading = new VitalReading(userId, request.type(), clock.instant());
        apply(reading, request);
        return toResponse(readings.save(reading), rangeService.flaggerFor(userId));
    }

    @Transactional
    public VitalReadingResponse update(Long userId, Long id, VitalReadingRequest request) {
        VitalValidator.validate(request, clock.instant());
        VitalReading reading = find(userId, id);
        reading.setType(request.type());
        apply(reading, request);
        return toResponse(reading, rangeService.flaggerFor(userId));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        readings.delete(find(userId, id));
    }

    @Transactional(readOnly = true)
    public VitalReadingResponse get(Long userId, Long id) {
        return toResponse(find(userId, id), rangeService.flaggerFor(userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<VitalReadingResponse> list(
            Long userId, VitalType type, LocalDate from, LocalDate to, boolean flaggedOnly, int page, int size) {
        return PageResponse.of(readings(userId, type, from, to, flaggedOnly), page, size);
    }

    /** Readings in a date range, newest first, with their flags. */
    @Transactional(readOnly = true)
    public List<VitalReadingResponse> readings(
            Long userId, VitalType type, LocalDate from, LocalDate to, boolean flaggedOnly) {
        DateRange range = DateRange.of(from, to, userService.zoneOf(userId));
        RangeFlagger flagger = rangeService.flaggerFor(userId);
        return readings.findInRange(userId, type, range.start(), range.end()).stream()
                .map(r -> toResponse(r, flagger))
                .filter(r -> !flaggedOnly || (r.flag() != null && r.flag().outOfRange()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> trend(Long userId, VitalType type, LocalDate from, LocalDate to) {
        ZoneId zone = userService.zoneOf(userId);
        DateRange range = DateRange.of(from, to, zone);
        List<VitalReading> list = readings.findInRange(userId, type, range.start(), range.end());
        return TrendAggregator.daily(list, zone, rangeService.flaggerFor(userId));
    }

    @Transactional(readOnly = true)
    public List<VitalSummary> summary(Long userId) {
        RangeFlagger flagger = rangeService.flaggerFor(userId);
        Instant now = clock.instant();
        Instant since = now.minus(Duration.ofDays(30));
        List<VitalSummary> result = new ArrayList<>();
        for (VitalType type : VitalType.values()) {
            VitalReadingResponse latest = readings.findFirstByUserIdAndTypeOrderByMeasuredAtDesc(userId, type)
                    .map(r -> toResponse(r, flagger))
                    .orElse(null);
            List<VitalReading> recent = readings.findInRange(userId, type, since, now.plusSeconds(1));
            int flagged = (int) recent.stream()
                    .map(r -> flagger.flag(r.getType(), r.getValue(), r.getSecondaryValue(), r.getGlucoseContext()))
                    .filter(TrendAggregator::isFlagged)
                    .count();
            result.add(new VitalSummary(
                    type,
                    type.unit(),
                    latest,
                    TrendAggregator.average(recent.stream().map(VitalReading::getValue).toList()),
                    TrendAggregator.average(recent.stream()
                            .map(VitalReading::getSecondaryValue)
                            .filter(Objects::nonNull)
                            .toList()),
                    recent.size(),
                    flagged));
        }
        return result;
    }

    private VitalReading find(Long userId, Long id) {
        return readings.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Reading"));
    }

    private static void apply(VitalReading reading, VitalReadingRequest request) {
        reading.setValue(request.value());
        reading.setSecondaryValue(request.secondaryValue());
        reading.setGlucoseContext(request.glucoseContext());
        reading.setMeasuredAt(request.measuredAt());
        reading.setNotes(blankToNull(request.notes()));
    }

    static VitalReadingResponse toResponse(VitalReading r, RangeFlagger flagger) {
        Flag flag = flagger.flag(r.getType(), r.getValue(), r.getSecondaryValue(), r.getGlucoseContext());
        return VitalReadingResponse.from(r, flag);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
