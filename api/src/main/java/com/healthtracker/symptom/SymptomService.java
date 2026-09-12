package com.healthtracker.symptom;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.DateRange;
import com.healthtracker.common.PageResponse;
import com.healthtracker.symptom.dto.SymptomRequest;
import com.healthtracker.symptom.dto.SymptomResponse;
import com.healthtracker.user.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SymptomService {

    private final SymptomRepository symptoms;
    private final UserService userService;
    private final Clock clock;

    public SymptomService(SymptomRepository symptoms, UserService userService, Clock clock) {
        this.symptoms = symptoms;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<SymptomResponse> list(
            Long userId, LocalDate from, LocalDate to, String name, int page, int size) {
        return PageResponse.of(entries(userId, from, to, name), page, size);
    }

    /** Entries in a date range, newest first. */
    @Transactional(readOnly = true)
    public List<SymptomResponse> entries(Long userId, LocalDate from, LocalDate to, String name) {
        DateRange range = DateRange.of(from, to, userService.zoneOf(userId));
        String wanted = name == null || name.isBlank() ? null : name.trim().toLowerCase(Locale.ROOT);
        return symptoms
                .findByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                        userId, range.start(), range.end())
                .stream()
                .filter(s -> wanted == null || s.getName().toLowerCase(Locale.ROOT).equals(wanted))
                .map(SymptomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> names(Long userId) {
        return symptoms.findNamesByFrequency(userId);
    }

    @Transactional
    public SymptomResponse create(Long userId, SymptomRequest request) {
        Symptom symptom = new Symptom(userId, clock.instant());
        apply(symptom, request);
        return SymptomResponse.from(symptoms.save(symptom));
    }

    @Transactional
    public SymptomResponse update(Long userId, Long id, SymptomRequest request) {
        Symptom symptom = find(userId, id);
        apply(symptom, request);
        return SymptomResponse.from(symptom);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        symptoms.delete(find(userId, id));
    }

    private Symptom find(Long userId, Long id) {
        return symptoms.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Symptom"));
    }

    private void apply(Symptom symptom, SymptomRequest request) {
        if (request.occurredAt().isAfter(clock.instant().plusSeconds(600))) {
            throw ApiException.badRequest("Symptoms cannot be logged in the future");
        }
        symptom.setName(canonicalName(symptom.getUserId(), request.name()));
        symptom.setSeverity(request.severity());
        symptom.setOccurredAt(request.occurredAt());
        symptom.setDurationMinutes(request.durationMinutes());
        symptom.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        symptom.setTags(normalizeTags(request.tags()));
    }

    /** Lower case, single spaces, no duplicates: "Stress", " stress " and "STRESS" are one tag. */
    static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(t -> t.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT))
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * Reuses the spelling of an earlier entry with the same name, so "HEADACHE" and "headache" end
     * up as one symptom in the suggestions and charts.
     */
    private String canonicalName(Long userId, String raw) {
        String name = raw.trim().replaceAll("\\s+", " ");
        return symptoms.findFirstByUserIdAndNameIgnoreCaseOrderByOccurredAtDesc(userId, name)
                .map(Symptom::getName)
                .orElseGet(() -> capitalize(name));
    }

    /** "headache" and "Headache" should group together in the charts. */
    static String capitalize(String name) {
        if (name.isEmpty()) {
            return name;
        }
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }
}
