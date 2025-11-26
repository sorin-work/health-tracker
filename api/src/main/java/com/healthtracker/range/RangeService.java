package com.healthtracker.range;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RangeService {

    /** The ranges used to flag a user's readings. Defaults for now. */
    public Map<RangeMetric, ReferenceRange> effectiveRanges(Long userId) {
        Map<RangeMetric, ReferenceRange> ranges = new EnumMap<>(RangeMetric.class);
        for (RangeMetric metric : RangeMetric.values()) {
            ranges.put(metric, metric.defaults());
        }
        return ranges;
    }

    public RangeFlagger flaggerFor(Long userId) {
        return new RangeFlagger(effectiveRanges(userId));
    }
}
