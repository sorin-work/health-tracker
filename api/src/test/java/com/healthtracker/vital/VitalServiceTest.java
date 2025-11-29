package com.healthtracker.vital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.PageResponse;
import com.healthtracker.range.Flag;
import com.healthtracker.range.RangeFlagger;
import com.healthtracker.range.RangeService;
import com.healthtracker.user.UserService;
import com.healthtracker.vital.dto.VitalReadingRequest;
import com.healthtracker.vital.dto.VitalReadingResponse;
import com.healthtracker.vital.dto.VitalSummary;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VitalServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-10T12:00:00Z");
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @Mock
    VitalReadingRepository repository;

    @Mock
    RangeService rangeService;

    @Mock
    UserService userService;

    VitalService service;

    @BeforeEach
    void setUp() {
        service = new VitalService(repository, rangeService, userService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static VitalReading reading(long id, VitalType type, String value, String secondary, Instant at) {
        VitalReading r = new VitalReading(1L, type, at);
        r.setId(id);
        r.setValue(new BigDecimal(value));
        r.setSecondaryValue(secondary == null ? null : new BigDecimal(secondary));
        r.setMeasuredAt(at);
        return r;
    }

    @Test
    void createsAReadingAndReturnsItsFlag() {
        when(rangeService.flaggerFor(1L)).thenReturn(RangeFlagger.withDefaults());
        when(repository.save(any(VitalReading.class))).thenAnswer(inv -> {
            VitalReading r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        VitalReadingResponse response = service.create(1L, new VitalReadingRequest(
                VitalType.BLOOD_PRESSURE, new BigDecimal("145"), new BigDecimal("92"), null, NOW, "  after coffee "));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.flag()).isEqualTo(Flag.HIGH);
        assertThat(response.unit()).isEqualTo("mmHg");
        assertThat(response.notes()).isEqualTo("after coffee");
    }

    @Test
    void invalidReadingsAreNotSaved() {
        assertThatThrownBy(() -> service.create(1L, new VitalReadingRequest(
                        VitalType.BLOOD_PRESSURE, new BigDecimal("145"), null, null, NOW, null)))
                .isInstanceOf(ApiException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void cannotTouchAnotherUsersReading() {
        when(repository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 5L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void listCanFilterToFlaggedReadingsOnly() {
        when(userService.zoneOf(1L)).thenReturn(ZoneOffset.UTC);
        when(rangeService.flaggerFor(1L)).thenReturn(RangeFlagger.withDefaults());
        when(repository.findInRange(eq(1L), eq(VitalType.HEART_RATE), any(), any())).thenReturn(List.of(
                reading(3, VitalType.HEART_RATE, "110", null, NOW.minusSeconds(60)),
                reading(2, VitalType.HEART_RATE, "70", null, NOW.minusSeconds(3600)),
                reading(1, VitalType.HEART_RATE, "45", null, NOW.minusSeconds(7200))));

        PageResponse<VitalReadingResponse> page = service.list(
                1L, VitalType.HEART_RATE, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10), true, 0, 50);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).extracting(VitalReadingResponse::flag).containsExactly(Flag.HIGH, Flag.LOW);
    }

    @Test
    void dateFilterCoversWholeDaysInTheUsersZone() {
        when(userService.zoneOf(1L)).thenReturn(BERLIN);
        when(rangeService.flaggerFor(1L)).thenReturn(RangeFlagger.withDefaults());

        service.list(1L, null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 7), false, 0, 50);

        verify(repository).findInRange(
                1L, null, Instant.parse("2026-01-31T23:00:00Z"), Instant.parse("2026-02-07T23:00:00Z"));
    }

    @Test
    void summaryHasAnEntryForEveryType() {
        when(rangeService.flaggerFor(1L)).thenReturn(RangeFlagger.withDefaults());
        when(repository.findFirstByUserIdAndTypeOrderByMeasuredAtDesc(eq(1L), any())).thenReturn(Optional.empty());
        when(repository.findFirstByUserIdAndTypeOrderByMeasuredAtDesc(1L, VitalType.HEART_RATE))
                .thenReturn(Optional.of(reading(1, VitalType.HEART_RATE, "104", null, NOW)));
        when(repository.findInRange(eq(1L), any(), any(), any())).thenReturn(List.of());
        when(repository.findInRange(eq(1L), eq(VitalType.HEART_RATE), any(), any())).thenReturn(List.of(
                reading(1, VitalType.HEART_RATE, "104", null, NOW),
                reading(2, VitalType.HEART_RATE, "70", null, NOW.minusSeconds(86400))));

        List<VitalSummary> summary = service.summary(1L);

        assertThat(summary).hasSize(VitalType.values().length);
        VitalSummary hr = summary.stream().filter(s -> s.type() == VitalType.HEART_RATE).findFirst().orElseThrow();
        assertThat(hr.latest().flag()).isEqualTo(Flag.HIGH);
        assertThat(hr.average30d()).isEqualByComparingTo("87.0");
        assertThat(hr.count30d()).isEqualTo(2);
        assertThat(hr.flagged30d()).isEqualTo(1);
        VitalSummary weight = summary.stream().filter(s -> s.type() == VitalType.WEIGHT).findFirst().orElseThrow();
        assertThat(weight.latest()).isNull();
        assertThat(weight.average30d()).isNull();
    }
}
