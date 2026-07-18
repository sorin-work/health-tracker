package com.healthtracker.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthtracker.journal.dto.JournalRequest;
import com.healthtracker.journal.dto.JournalResponse;
import com.healthtracker.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    /** Already 2026-07-03 in Tokyo. */
    private static final Instant NOW = Instant.parse("2026-07-02T16:00:00Z");

    @Mock
    JournalRepository journal;

    @Mock
    UserService userService;

    JournalService service;

    @BeforeEach
    void setUp() {
        service = new JournalService(journal, userService, Clock.fixed(NOW, TOKYO));
        lenient().when(userService.zoneOf(1L)).thenReturn(TOKYO);
        lenient().when(journal.save(any(JournalEntry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createsAnEntryForTodayInTheUsersZone() {
        JournalResponse created = service.create(1L, new JournalRequest(
                LocalDate.of(2026, 7, 3), "  New dose  ", "\n Started 20 mg today. Slight headache. \n"));

        assertThat(created.entryDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(created.title()).isEqualTo("New dose");
        assertThat(created.body()).isEqualTo("Started 20 mg today. Slight headache.");
    }

    @Test
    void rejectsEntriesDatedInTheFuture() {
        assertThatThrownBy(() -> service.create(1L, new JournalRequest(LocalDate.of(2026, 7, 4), null, "x")))
                .hasMessageContaining("future");
    }

    @Test
    void updateStampsTheEditTime() {
        JournalEntry entry = new JournalEntry(1L, NOW.minusSeconds(86_400));
        entry.setEntryDate(LocalDate.of(2026, 7, 1));
        entry.setBody("Old");
        when(journal.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(entry));

        JournalResponse updated = service.update(1L, 5L, new JournalRequest(LocalDate.of(2026, 7, 1), "", "New"));

        assertThat(updated.body()).isEqualTo("New");
        assertThat(updated.title()).isNull();
        assertThat(updated.updatedAt()).isEqualTo(NOW);
        assertThat(updated.createdAt()).isEqualTo(NOW.minusSeconds(86_400));
    }

    @Test
    void blankSearchMeansNoFilter() {
        service.entries(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), "   ");

        verify(journal).search(any(), any(), any(), isNull());
    }
}
