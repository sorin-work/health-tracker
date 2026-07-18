package com.healthtracker.journal;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.DateRange;
import com.healthtracker.common.PageResponse;
import com.healthtracker.journal.dto.JournalRequest;
import com.healthtracker.journal.dto.JournalResponse;
import com.healthtracker.user.UserService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalService {

    private final JournalRepository journal;
    private final UserService userService;
    private final Clock clock;

    public JournalService(JournalRepository journal, UserService userService, Clock clock) {
        this.journal = journal;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<JournalResponse> list(
            Long userId, LocalDate from, LocalDate to, String query, int page, int size) {
        return PageResponse.of(entries(userId, from, to, query), page, size);
    }

    /** Entries between two days (inclusive), newest first, optionally matching some text. */
    @Transactional(readOnly = true)
    public List<JournalResponse> entries(Long userId, LocalDate from, LocalDate to, String query) {
        DateRange range = DateRange.of(from, to, userService.zoneOf(userId));
        String text = query == null || query.isBlank() ? null : query.trim();
        return journal.search(userId, range.from(), range.to(), text).stream()
                .map(JournalResponse::from)
                .toList();
    }

    @Transactional
    public JournalResponse create(Long userId, JournalRequest request) {
        JournalEntry entry = new JournalEntry(userId, clock.instant());
        apply(userId, entry, request);
        return JournalResponse.from(journal.save(entry));
    }

    @Transactional
    public JournalResponse update(Long userId, Long id, JournalRequest request) {
        JournalEntry entry = find(userId, id);
        apply(userId, entry, request);
        entry.setUpdatedAt(clock.instant());
        return JournalResponse.from(entry);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        journal.delete(find(userId, id));
    }

    private JournalEntry find(Long userId, Long id) {
        return journal.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Journal entry"));
    }

    private void apply(Long userId, JournalEntry entry, JournalRequest request) {
        ZoneId zone = userService.zoneOf(userId);
        if (request.entryDate().isAfter(LocalDate.now(clock.withZone(zone)))) {
            throw ApiException.badRequest("Journal entries cannot be dated in the future");
        }
        entry.setEntryDate(request.entryDate());
        entry.setTitle(request.title() == null || request.title().isBlank() ? null : request.title().trim());
        entry.setBody(request.body().strip());
    }
}
