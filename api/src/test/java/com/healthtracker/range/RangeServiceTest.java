package com.healthtracker.range;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthtracker.common.ApiException;
import com.healthtracker.range.dto.RangeResponse;
import com.healthtracker.range.dto.UpdateRangeRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RangeServiceTest {

    @Mock
    CustomRangeRepository repository;

    RangeService service;

    @BeforeEach
    void setUp() {
        service = new RangeService(repository, Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void listsDefaultsWhenNothingIsCustomised() {
        when(repository.findByUserId(1L)).thenReturn(List.of());

        List<RangeResponse> ranges = service.list(1L);

        assertThat(ranges).hasSize(RangeMetric.values().length);
        RangeResponse hr = ranges.stream().filter(r -> r.metric().equals("HEART_RATE")).findFirst().orElseThrow();
        assertThat(hr.low()).isEqualByComparingTo("50");
        assertThat(hr.high()).isEqualByComparingTo("100");
        assertThat(hr.custom()).isFalse();
    }

    @Test
    void customRangeOverridesDefaults() {
        CustomRange custom = new CustomRange(1L, RangeMetric.GLUCOSE_FASTING);
        custom.update(new BigDecimal("80"), new BigDecimal("130"), Instant.now());
        when(repository.findByUserId(1L)).thenReturn(List.of(custom));

        ReferenceRange effective = service.effectiveRanges(1L).get(RangeMetric.GLUCOSE_FASTING);

        assertThat(effective.low()).isEqualByComparingTo("80");
        assertThat(effective.high()).isEqualByComparingTo("130");
        assertThat(effective.criticalHigh()).isEqualByComparingTo("250");
    }

    @Test
    void updateCreatesOrReplacesTheCustomRange() {
        when(repository.findByUserIdAndMetric(1L, RangeMetric.SYSTOLIC)).thenReturn(Optional.empty());

        RangeResponse response = service.update(
                1L, RangeMetric.SYSTOLIC, new UpdateRangeRequest(new BigDecimal("95"), new BigDecimal("135")));

        assertThat(response.custom()).isTrue();
        assertThat(response.high()).isEqualByComparingTo("135");
        assertThat(response.defaultHigh()).isEqualByComparingTo("129");
        verify(repository).save(any(CustomRange.class));
    }

    @Test
    void rejectsInvertedBounds() {
        assertThatThrownBy(() -> service.update(
                        1L, RangeMetric.SYSTOLIC, new UpdateRangeRequest(new BigDecimal("140"), new BigDecimal("120"))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("below");
        verify(repository, never()).save(any());
    }
}
