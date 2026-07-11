package com.healthtracker.vital;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthtracker.common.ApiException;
import com.healthtracker.common.PageResponse;
import com.healthtracker.config.SecurityConfig;
import com.healthtracker.range.Flag;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import com.healthtracker.vital.dto.TrendPoint;
import com.healthtracker.vital.dto.VitalReadingResponse;
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

@WebMvcTest(VitalController.class)
@Import(SecurityConfig.class)
class VitalControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VitalService vitalService;

    @MockitoBean
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    private static VitalReadingResponse bp() {
        return new VitalReadingResponse(7L, VitalType.BLOOD_PRESSURE, new BigDecimal("142"), new BigDecimal("91"),
                "mmHg", null, Instant.parse("2026-02-01T07:30:00Z"), null, Flag.HIGH);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/vitals")).andExpect(status().isUnauthorized());
        verifyNoInteractions(vitalService);
    }

    @Test
    void listPassesFiltersThrough() throws Exception {
        when(vitalService.list(1L, VitalType.BLOOD_PRESSURE, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), true, 0, 20))
                .thenReturn(new PageResponse<>(List.of(bp()), 0, 20, 1));

        mvc.perform(get("/api/vitals")
                        .param("type", "BLOOD_PRESSURE")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .param("flagged", "true")
                        .param("size", "20")
                        .with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].flag").value("HIGH"))
                .andExpect(jsonPath("$.items[0].secondaryValue").value(91))
                .andExpect(jsonPath("$.items[0].glucoseContext").doesNotExist());
    }

    @Test
    void unknownTypeIs400() throws Exception {
        mvc.perform(get("/api/vitals").param("type", "MOOD").with(TestAuth.signedIn()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'type'"));
    }

    @Test
    void createReturns201() throws Exception {
        when(vitalService.create(eq(1L), any())).thenReturn(bp());

        mvc.perform(post("/api/vitals")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"BLOOD_PRESSURE","value":142,"secondaryValue":91,"measuredAt":"2026-02-01T07:30:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.unit").value("mmHg"));
    }

    @Test
    void createValidatesRequiredFields() throws Exception {
        mvc.perform(post("/api/vitals")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WEIGHT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.value").exists())
                .andExpect(jsonPath("$.fields.measuredAt").exists());
    }

    @Test
    void serviceErrorsKeepTheirMessage() throws Exception {
        when(vitalService.create(eq(1L), any()))
                .thenThrow(ApiException.badRequest("Blood pressure needs both systolic and diastolic values"));

        mvc.perform(post("/api/vitals")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"BLOOD_PRESSURE\",\"value\":120,\"measuredAt\":\"2026-02-01T07:30:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Blood pressure needs both systolic and diastolic values"));
    }

    @Test
    void trendNeedsAType() throws Exception {
        mvc.perform(get("/api/vitals/trend").with(TestAuth.signedIn()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing parameter 'type'"));
    }

    @Test
    void trendReturnsDailyPoints() throws Exception {
        when(vitalService.trend(1L, VitalType.WEIGHT, null, null)).thenReturn(List.of(new TrendPoint(
                LocalDate.of(2026, 2, 1), new BigDecimal("82.3"), new BigDecimal("82.1"), new BigDecimal("82.5"),
                null, 2, 0)));

        mvc.perform(get("/api/vitals/trend").param("type", "WEIGHT").with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-02-01"))
                .andExpect(jsonPath("$[0].average").value(82.3));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/vitals/7").with(TestAuth.signedIn())).andExpect(status().isNoContent());
        verify(vitalService).delete(1L, 7L);
    }

    @Test
    void missingReadingIs404() throws Exception {
        when(vitalService.get(1L, 99L)).thenThrow(ApiException.notFound("Reading"));

        mvc.perform(get("/api/vitals/99").with(TestAuth.signedIn()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Reading not found"));
    }
}
