package com.healthtracker.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthtracker.user.UserService;
import com.healthtracker.vital.GlucoseContext;
import com.healthtracker.vital.VitalReading;
import com.healthtracker.vital.VitalReadingRepository;
import com.healthtracker.vital.VitalType;
import com.healthtracker.vital.dto.VitalReadingRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VitalImportServiceTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");
    private static final String HEADER = "measured_at,type,value,secondary_value,unit,glucose_context,flag,notes\r\n";

    @Mock
    VitalReadingRepository readings;

    @Mock
    UserService userService;

    VitalImportService service;

    @BeforeEach
    void setUp() {
        service = new VitalImportService(readings, userService, Clock.fixed(NOW, BERLIN));
        lenient().when(userService.zoneOf(1L)).thenReturn(BERLIN);
        lenient().when(readings.findInRange(eq(1L), isNull(), any(), any())).thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private List<VitalReading> saved() {
        ArgumentCaptor<List<VitalReading>> captor = ArgumentCaptor.forClass(List.class);
        verify(readings).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void importsWhatTheExportWrites() {
        String csv = HEADER
                + "2026-09-01 07:30,WEIGHT,82.40,,kg,,,\r\n"
                + "2026-09-01 08:15,BLOOD_PRESSURE,142,91,mmHg,,HIGH,\"after a run, tired\"\r\n"
                + "2026-09-02 06:40,BLOOD_GLUCOSE,98,,mg/dL,FASTING,NORMAL,\r\n";

        ImportResult result = service.importCsv(1L, csv, false);

        assertThat(result.dryRun()).isFalse();
        assertThat(result.rows()).isEqualTo(3);
        assertThat(result.imported()).isEqualTo(3);
        assertThat(result.errors()).isEmpty();
        List<VitalReading> saved = saved();
        assertThat(saved.get(0).getMeasuredAt()).isEqualTo(Instant.parse("2026-09-01T05:30:00Z"));
        assertThat(saved.get(1).getSecondaryValue()).isEqualByComparingTo("91");
        assertThat(saved.get(1).getNotes()).isEqualTo("after a run, tired");
        assertThat(saved.get(2).getGlucoseContext()).isEqualTo(GlucoseContext.FASTING);
    }

    @Test
    void columnsCanBeInAnyOrderAndOptionalOnesLeftOut() {
        ImportResult result = service.importCsv(1L, "Value,Type,Measured_At\n7.5,sleep,2026-09-03T06:30:00Z\n", false);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(saved().getFirst().getType()).isEqualTo(VitalType.SLEEP);
    }

    @Test
    void aDryRunSavesNothing() {
        ImportResult result = service.importCsv(1L, HEADER + "2026-09-01 07:30,WEIGHT,82.4,,,,,\r\n", true);

        assertThat(result.dryRun()).isTrue();
        assertThat(result.imported()).isZero();
        assertThat(result.preview()).extracting(VitalReadingRequest::value).containsExactly(new BigDecimal("82.4"));
        verify(readings, never()).saveAll(anyList());
    }

    @Test
    void anyInvalidRowStopsTheWholeImport() {
        String csv = HEADER
                + "2026-09-01 07:30,WEIGHT,82.4,,,,,\r\n"
                + "2026-09-01 07:31,WEIGHT,8200,,,,,\r\n"
                + "yesterday,WEIGHT,82,,,,,\r\n"
                + "2026-09-01 07:33,TEMPERATURE,37,,,,,\r\n"
                + "2026-09-01 07:34,BLOOD_PRESSURE,120,,,,,\r\n";

        ImportResult result = service.importCsv(1L, csv, false);

        assertThat(result.dryRun()).isTrue();
        assertThat(result.imported()).isZero();
        assertThat(result.errors()).extracting(ImportResult.RowError::line).containsExactly(3, 4, 5, 6);
        assertThat(result.errors().get(0).message()).contains("Weight must be between");
        assertThat(result.errors().get(1).message()).contains("2026-02-01 07:30");
        assertThat(result.errors().get(2).message()).isEqualTo("Unknown type or meal context");
        assertThat(result.errors().get(3).message()).contains("diastolic");
        verify(readings, never()).saveAll(anyList());
    }

    @Test
    void skipsReadingsThatAreAlreadyStored() {
        VitalReading existing = new VitalReading(1L, VitalType.WEIGHT, NOW);
        existing.setValue(new BigDecimal("82.40"));
        existing.setMeasuredAt(Instant.parse("2026-09-01T05:30:00Z"));
        when(readings.findInRange(eq(1L), isNull(), any(), any())).thenReturn(List.of(existing));
        String csv = HEADER
                + "2026-09-01 07:30,WEIGHT,82.4,,,,,\r\n"
                + "2026-09-02 07:30,WEIGHT,82.1,,,,,\r\n"
                + "2026-09-02 07:30,WEIGHT,82.1,,,,,\r\n";

        ImportResult result = service.importCsv(1L, csv, false);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.duplicates()).isEqualTo(2);
    }

    @Test
    void undoesTheExportsFormulaProtection() {
        service.importCsv(1L, HEADER + "2026-09-01 07:30,WEIGHT,82.4,,,,,'-2 kg since Monday\r\n", false);

        assertThat(saved().getFirst().getNotes()).isEqualTo("-2 kg since Monday");
    }

    @Test
    void needsTheRequiredColumns() {
        assertThatThrownBy(() -> service.importCsv(1L, "when,what\n2026-09-01,WEIGHT\n", false))
                .hasMessageContaining("'measured_at'");
        assertThatThrownBy(() -> service.importCsv(1L, "", false)).hasMessageContaining("empty");
    }
}
