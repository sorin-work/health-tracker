package com.healthtracker.mood;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthtracker.config.SecurityConfig;
import com.healthtracker.mood.dto.MoodDay;
import com.healthtracker.mood.dto.MoodResponse;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import java.math.BigDecimal;
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

@WebMvcTest(MoodController.class)
@Import(SecurityConfig.class)
class MoodControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MoodService moodService;

    @MockitoBean
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/mood")).andExpect(status().isUnauthorized());
    }

    @Test
    void createsACheckIn() throws Exception {
        when(moodService.create(eq(1L), any()))
                .thenReturn(new MoodResponse(9L, 4, 3, Instant.parse("2026-05-20T08:00:00Z"), null));

        mvc.perform(post("/api/mood")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mood":4,"energy":3,"recordedAt":"2026-05-20T08:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.mood").value(4));
    }

    @Test
    void moodMustBeOneToFive() throws Exception {
        mvc.perform(post("/api/mood")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mood":6,"recordedAt":"2026-05-20T08:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.mood").exists());
        verifyNoInteractions(moodService);
    }

    @Test
    void returnsDailyAverages() throws Exception {
        when(moodService.daily(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))).thenReturn(List.of(
                new MoodDay(LocalDate.of(2026, 5, 20), new BigDecimal("3.5"), null, 2)));

        mvc.perform(get("/api/mood/daily?from=2026-05-01&to=2026-05-31").with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-05-20"))
                .andExpect(jsonPath("$[0].mood").value(3.5))
                .andExpect(jsonPath("$[0].energy").doesNotExist());
    }
}
