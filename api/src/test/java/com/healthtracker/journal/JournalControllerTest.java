package com.healthtracker.journal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthtracker.common.PageResponse;
import com.healthtracker.config.SecurityConfig;
import com.healthtracker.journal.dto.JournalResponse;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JournalController.class)
@Import(SecurityConfig.class)
class JournalControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JournalService journalService;

    @MockitoBean
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    private static JournalResponse note() {
        Instant at = Instant.parse("2026-07-01T20:00:00Z");
        return new JournalResponse(3L, LocalDate.of(2026, 7, 1), null, "Ask about the dizziness", at, at);
    }

    @Test
    void searchesEntries() throws Exception {
        when(journalService.list(1L, null, null, "dizz", 0, 20)).thenReturn(PageResponse.of(List.of(note()), 0, 20));

        mvc.perform(get("/api/journal?q=dizz").with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].body").value("Ask about the dizziness"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void createsAnEntry() throws Exception {
        when(journalService.create(eq(1L), any())).thenReturn(note());

        mvc.perform(post("/api/journal")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entryDate":"2026-07-01","body":"Ask about the dizziness"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void bodyIsRequired() throws Exception {
        mvc.perform(post("/api/journal")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entryDate":"2026-07-01","body":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.body").exists());
        verifyNoInteractions(journalService);
    }
}
