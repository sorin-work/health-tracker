package com.healthtracker.journal;

import com.healthtracker.common.PageResponse;
import com.healthtracker.journal.dto.JournalRequest;
import com.healthtracker.journal.dto.JournalResponse;
import com.healthtracker.security.AuthUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/journal")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping
    public PageResponse<JournalResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return journalService.list(user.id(), from, to, q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JournalResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody JournalRequest request) {
        return journalService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public JournalResponse update(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id, @Valid @RequestBody JournalRequest request) {
        return journalService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        journalService.delete(user.id(), id);
    }
}
