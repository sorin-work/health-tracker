package com.healthtracker.medication;

import static com.healthtracker.medication.MedicationFixtures.scheduled;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.healthtracker.common.ApiException;
import com.healthtracker.medication.dto.MedicationRequest;
import com.healthtracker.medication.dto.MedicationResponse;
import com.healthtracker.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-20T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 20);

    @Mock
    MedicationRepository repository;

    @Mock
    UserService userService;

    MedicationService service;

    @BeforeEach
    void setUp() {
        service = new MedicationService(repository, userService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static MedicationRequest request(boolean asNeeded, List<LocalTime> times, LocalDate end) {
        return new MedicationRequest(" Metformin ", "500 mg", null, asNeeded, times, TODAY, end);
    }

    @Test
    void createSortsAndDeduplicatesTimes() {
        when(repository.save(any(Medication.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicationResponse response = service.create(1L, request(
                false, List.of(LocalTime.of(20, 0), LocalTime.of(8, 0), LocalTime.of(8, 0, 30)), null));

        assertThat(response.name()).isEqualTo("Metformin");
        assertThat(response.times()).containsExactly(LocalTime.of(8, 0), LocalTime.of(20, 0));
        assertThat(response.active()).isTrue();
    }

    @Test
    void scheduledMedicationNeedsATime() {
        assertThatThrownBy(() -> service.create(1L, request(false, List.of(), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("at least one time");
    }

    @Test
    void asNeededMedicationDropsTimes() {
        when(repository.save(any(Medication.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicationResponse response = service.create(1L, request(true, List.of(LocalTime.of(8, 0)), null));

        assertThat(response.asNeeded()).isTrue();
        assertThat(response.times()).isEmpty();
    }

    @Test
    void endDateCannotPrecedeStart() {
        assertThatThrownBy(() -> service.create(1L, request(false, List.of(LocalTime.NOON), TODAY.minusDays(1))))
                .hasMessageContaining("End date");
    }

    @Test
    void archiveEndsTheCourseToday() {
        Medication m = scheduled(1, "Metformin", TODAY.minusDays(40), null, "08:00");
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(m));
        when(userService.zoneOf(1L)).thenReturn(ZoneOffset.UTC);

        MedicationResponse response = service.archive(1L, 1L);

        assertThat(response.active()).isFalse();
        assertThat(response.endDate()).isEqualTo(TODAY);
    }

    @Test
    void archiveKeepsAnEarlierEndDate() {
        Medication m = scheduled(1, "Amoxicillin", TODAY.minusDays(40), TODAY.minusDays(30), "08:00");
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(m));
        when(userService.zoneOf(1L)).thenReturn(ZoneOffset.UTC);

        assertThat(service.archive(1L, 1L).endDate()).isEqualTo(TODAY.minusDays(30));
    }
}
