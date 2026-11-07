package com.healthtracker.symptom;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthtracker.common.PageResponse;
import com.healthtracker.config.SecurityConfig;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import com.healthtracker.symptom.dto.SymptomResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SymptomController.class)
@Import(SecurityConfig.class)
class SymptomControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SymptomService symptomService;

    @MockitoBean
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    private static SymptomResponse migraine() {
        return new SymptomResponse(4L, "Migraine", 7, Instant.parse("2026-09-01T10:00:00Z"), 120, null,
                List.of("screen time", "stress"));
    }

    @Test
    void filtersByTag() throws Exception {
        when(symptomService.list(1L, null, null, null, "stress", 0, 50))
                .thenReturn(PageResponse.of(List.of(migraine()), 0, 50));

        mvc.perform(get("/api/symptoms?tag=stress").with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tags[1]").value("stress"));
    }

    @Test
    void listsTheTagsInUse() throws Exception {
        when(symptomService.tags(1L)).thenReturn(List.of("stress", "screen time"));

        mvc.perform(get("/api/symptoms/tags").with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("stress"));
    }

    @Test
    void createsAnEntryWithTags() throws Exception {
        when(symptomService.create(eq(1L), any())).thenReturn(migraine());

        mvc.perform(post("/api/symptoms")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Migraine","severity":7,"occurredAt":"2026-09-01T10:00:00Z",
                                 "tags":["stress","screen time"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4));
        verify(symptomService).create(eq(1L), any());
    }

    @Test
    void tagsHaveALengthLimit() throws Exception {
        mvc.perform(post("/api/symptoms")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Migraine","severity":7,"occurredAt":"2026-09-01T10:00:00Z",
                                 "tags":["this tag is far too long to be useful as a tag"]}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(symptomService);
    }

    @Test
    void severityMustBeOneToTen() throws Exception {
        mvc.perform(post("/api/symptoms")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Migraine","severity":11,"occurredAt":"2026-09-01T10:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.severity").exists());
    }
}
