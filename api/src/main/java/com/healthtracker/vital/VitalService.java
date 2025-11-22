package com.healthtracker.vital;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.DateRange;
import com.healthtracker.common.PageResponse;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.dto.VitalReadingRequest;
import com.healthtracker.vital.dto.VitalReadingResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VitalService {

    private final VitalReadingRepository readings;
    private final UserService userService;
    private final Clock clock;

    public VitalService(VitalReadingRepository readings, UserService userService, Clock clock) {
        this.readings = readings;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional
    public VitalReadingResponse create(Long userId, VitalReadingRequest request) {
        VitalValidator.validate(request, clock.instant());
        VitalReading reading = new VitalReading(userId, request.type(), clock.instant());
        apply(reading, request);
        return VitalReadingResponse.from(readings.save(reading));
    }

    @Transactional
    public VitalReadingResponse update(Long userId, Long id, VitalReadingRequest request) {
        VitalValidator.validate(request, clock.instant());
        VitalReading reading = find(userId, id);
        reading.setType(request.type());
        apply(reading, request);
        return VitalReadingResponse.from(reading);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        readings.delete(find(userId, id));
    }

    @Transactional(readOnly = true)
    public VitalReadingResponse get(Long userId, Long id) {
        return VitalReadingResponse.from(find(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<VitalReadingResponse> list(
            Long userId, VitalType type, LocalDate from, LocalDate to, int page, int size) {
        return PageResponse.of(readings(userId, type, from, to), page, size);
    }

    /** Readings in a date range, newest first. */
    @Transactional(readOnly = true)
    public List<VitalReadingResponse> readings(Long userId, VitalType type, LocalDate from, LocalDate to) {
        DateRange range = DateRange.of(from, to, userService.zoneOf(userId));
        return readings.findInRange(userId, type, range.start(), range.end()).stream()
                .map(VitalReadingResponse::from)
                .toList();
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

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
