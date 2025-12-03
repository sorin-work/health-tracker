package com.healthtracker.medication;

import com.healthtracker.medication.dto.MedicationRequest;
import com.healthtracker.medication.dto.MedicationResponse;
import com.healthtracker.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    public List<MedicationResponse> list(
            @AuthenticationPrincipal AuthUser user, @RequestParam(defaultValue = "false") boolean includeArchived) {
        return medicationService.list(user.id(), includeArchived);
    }

    @GetMapping("/{id}")
    public MedicationResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return medicationService.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicationResponse create(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody MedicationRequest request) {
        return medicationService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public MedicationResponse update(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id, @Valid @RequestBody MedicationRequest request) {
        return medicationService.update(user.id(), id, request);
    }

    @PostMapping("/{id}/archive")
    public MedicationResponse archive(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return medicationService.archive(user.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        medicationService.delete(user.id(), id);
    }
}
