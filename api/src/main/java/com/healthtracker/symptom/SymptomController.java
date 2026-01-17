package com.healthtracker.symptom;

import com.healthtracker.common.PageResponse;
import com.healthtracker.security.AuthUser;
import com.healthtracker.symptom.dto.SymptomRequest;
import com.healthtracker.symptom.dto.SymptomResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/symptoms")
public class SymptomController {

    private final SymptomService symptomService;

    public SymptomController(SymptomService symptomService) {
        this.symptomService = symptomService;
    }

    @GetMapping
    public PageResponse<SymptomResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return symptomService.list(user.id(), from, to, name, page, size);
    }

    @GetMapping("/names")
    public List<String> names(@AuthenticationPrincipal AuthUser user) {
        return symptomService.names(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SymptomResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody SymptomRequest request) {
        return symptomService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public SymptomResponse update(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id, @Valid @RequestBody SymptomRequest request) {
        return symptomService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        symptomService.delete(user.id(), id);
    }
}
