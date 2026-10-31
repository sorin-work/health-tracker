package com.healthtracker.vital;

import com.healthtracker.common.PageResponse;
import com.healthtracker.security.AuthUser;
import com.healthtracker.vital.dto.TrendPoint;
import com.healthtracker.vital.dto.VitalReadingRequest;
import com.healthtracker.vital.dto.VitalReadingResponse;
import com.healthtracker.vital.dto.VitalSummary;
import com.healthtracker.vital.dto.WeeklyChange;
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
@RequestMapping("/api/vitals")
public class VitalController {

    private final VitalService vitalService;

    public VitalController(VitalService vitalService) {
        this.vitalService = vitalService;
    }

    @GetMapping
    public PageResponse<VitalReadingResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) VitalType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean flagged,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return vitalService.list(user.id(), type, from, to, flagged, page, size);
    }

    @GetMapping("/summary")
    public List<VitalSummary> summary(@AuthenticationPrincipal AuthUser user) {
        return vitalService.summary(user.id());
    }

    /** Last 7 days against the 7 days before, per vital type. */
    @GetMapping("/weekly-change")
    public List<WeeklyChange> weeklyChange(@AuthenticationPrincipal AuthUser user) {
        return vitalService.weeklyChange(user.id());
    }

    @GetMapping("/trend")
    public List<TrendPoint> trend(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam VitalType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return vitalService.trend(user.id(), type, from, to);
    }

    @GetMapping("/{id}")
    public VitalReadingResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return vitalService.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VitalReadingResponse create(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody VitalReadingRequest request) {
        return vitalService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public VitalReadingResponse update(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id,
            @Valid @RequestBody VitalReadingRequest request) {
        return vitalService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        vitalService.delete(user.id(), id);
    }
}
