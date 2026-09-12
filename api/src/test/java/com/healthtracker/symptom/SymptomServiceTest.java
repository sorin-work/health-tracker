package com.healthtracker.symptom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.healthtracker.common.ApiException;
import com.healthtracker.symptom.dto.SymptomRequest;
import com.healthtracker.symptom.dto.SymptomResponse;
import com.healthtracker.user.UserService;
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
class SymptomServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-20T12:00:00Z");

    @Mock
    SymptomRepository symptoms;

    @Mock
    UserService userService;

    SymptomService service;

    @BeforeEach
    void setUp() {
        service = new SymptomService(symptoms, userService, Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(symptoms.save(any(Symptom.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(symptoms.findFirstByUserIdAndNameIgnoreCaseOrderByOccurredAtDesc(eq(1L), anyString()))
                .thenReturn(Optional.empty());
    }

    private static SymptomRequest request(String name) {
        return new SymptomRequest(name, 4, NOW.minusSeconds(3600), 30, null);
    }

    @Test
    void capitalizesANewSymptom() {
        SymptomResponse created = service.create(1L, request("  lower back   pain "));

        assertThat(created.name()).isEqualTo("Lower back pain");
    }

    @Test
    void reusesTheSpellingOfAnEarlierEntry() {
        Symptom earlier = new Symptom(1L, NOW);
        earlier.setName("Migraine");
        when(symptoms.findFirstByUserIdAndNameIgnoreCaseOrderByOccurredAtDesc(1L, "MIGRAINE"))
                .thenReturn(Optional.of(earlier));

        SymptomResponse created = service.create(1L, request("MIGRAINE"));

        assertThat(created.name()).isEqualTo("Migraine");
    }

    @Test
    void rejectsSymptomsInTheFuture() {
        SymptomRequest future = new SymptomRequest("Headache", 4, NOW.plusSeconds(3600), null, null);

        assertThatThrownBy(() -> service.create(1L, future))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("future");
    }

    @Test
    void tagsAreLowerCasedAndDeduplicated() {
        SymptomRequest withTags = new SymptomRequest(
                "Headache", 5, NOW.minusSeconds(60), null, null, List.of("Stress", " stress ", "after  coffee", " "));

        SymptomResponse created = service.create(1L, withTags);

        assertThat(created.tags()).containsExactly("after coffee", "stress");
    }

    @Test
    void filtersByTagIgnoringCase() {
        Symptom stress = new Symptom(1L, NOW);
        stress.setName("Headache");
        stress.setSeverity(5);
        stress.setOccurredAt(NOW.minusSeconds(3600));
        stress.setTags(List.of("stress"));
        Symptom other = new Symptom(1L, NOW);
        other.setName("Headache");
        other.setSeverity(3);
        other.setOccurredAt(NOW.minusSeconds(7200));
        when(userService.zoneOf(1L)).thenReturn(ZoneOffset.UTC);
        when(symptoms.findByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                        eq(1L), any(), any()))
                .thenReturn(List.of(stress, other));

        assertThat(service.list(1L, null, null, null, "Stress", 0, 50).items())
                .singleElement()
                .satisfies(s -> assertThat(s.severity()).isEqualTo(5));
    }
}
