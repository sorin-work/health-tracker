package com.healthtracker.range;

import com.healthtracker.common.ApiException;
import com.healthtracker.range.dto.RangeResponse;
import com.healthtracker.range.dto.UpdateRangeRequest;
import java.time.Clock;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RangeService {

    private final CustomRangeRepository customRanges;
    private final Clock clock;

    public RangeService(CustomRangeRepository customRanges, Clock clock) {
        this.customRanges = customRanges;
        this.clock = clock;
    }

    /** Defaults with the user's own bounds layered on top. */
    @Transactional(readOnly = true)
    public Map<RangeMetric, ReferenceRange> effectiveRanges(Long userId) {
        Map<RangeMetric, ReferenceRange> ranges = new EnumMap<>(RangeMetric.class);
        for (RangeMetric metric : RangeMetric.values()) {
            ranges.put(metric, metric.defaults());
        }
        for (CustomRange custom : customRanges.findByUserId(userId)) {
            ranges.put(custom.getMetric(), custom.getMetric().defaults().withBounds(custom.getLow(), custom.getHigh()));
        }
        return ranges;
    }

    public RangeFlagger flaggerFor(Long userId) {
        return new RangeFlagger(effectiveRanges(userId));
    }

    @Transactional(readOnly = true)
    public List<RangeResponse> list(Long userId) {
        Map<RangeMetric, ReferenceRange> effective = effectiveRanges(userId);
        List<RangeMetric> custom = customRanges.findByUserId(userId).stream().map(CustomRange::getMetric).toList();
        return Arrays.stream(RangeMetric.values())
                .map(m -> toResponse(m, effective.get(m), custom.contains(m)))
                .toList();
    }

    @Transactional
    public RangeResponse update(Long userId, RangeMetric metric, UpdateRangeRequest request) {
        if (request.low().compareTo(request.high()) >= 0) {
            throw ApiException.badRequest("Low bound must be below the high bound");
        }
        CustomRange range = customRanges.findByUserIdAndMetric(userId, metric)
                .orElseGet(() -> new CustomRange(userId, metric));
        range.update(request.low(), request.high(), clock.instant());
        customRanges.save(range);
        return toResponse(metric, metric.defaults().withBounds(request.low(), request.high()), true);
    }

    @Transactional
    public RangeResponse reset(Long userId, RangeMetric metric) {
        customRanges.findByUserIdAndMetric(userId, metric).ifPresent(customRanges::delete);
        return toResponse(metric, metric.defaults(), false);
    }

    private static RangeResponse toResponse(RangeMetric metric, ReferenceRange range, boolean custom) {
        return new RangeResponse(
                metric.name(),
                metric.label(),
                metric.unit(),
                range.low(),
                range.high(),
                range.criticalLow(),
                range.criticalHigh(),
                metric.defaults().low(),
                metric.defaults().high(),
                custom);
    }
}
