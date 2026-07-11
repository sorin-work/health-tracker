package com.healthtracker.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthtracker.config.SecurityConfig;
import com.healthtracker.medication.dto.AdherenceResponse;
import com.healthtracker.medication.dto.DoseResponse;
import com.healthtracker.medication.dto.MedicationRequest;
import com.healthtracker.medication.dto.MedicationResponse;
import com.healthtracker.medication.dto.RecordDoseRequest;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({MedicationController.class, DoseController.class})
@Import(SecurityConfig.class)
class MedicationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MedicationService medicationService;

    @MockitoBean
    DoseService doseService;

    @MockitoBean
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    @Test
    void createParsesTimesOfDay() throws Exception {
        when(medicationService.create(eq(1L), any())).thenReturn(new MedicationResponse(
                3L, "Metformin", "500 mg", null, false, List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                LocalDate.of(2026, 1, 1), null, true));

        mvc.perform(post("/api/medications")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Metformin","dosage":"500 mg","times":["08:00","20:00"],"startDate":"2026-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));

        ArgumentCaptor<MedicationRequest> captor = ArgumentCaptor.forClass(MedicationRequest.class);
        verify(medicationService).create(eq(1L), captor.capture());
        assertThat(captor.getValue().times()).containsExactly(LocalTime.of(8, 0), LocalTime.of(20, 0));
        assertThat(captor.getValue().asNeeded()).isFalse();
    }

    @Test
    void createNeedsANameAndStartDate() throws Exception {
        mvc.perform(post("/api/medications")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"times\":[\"08:00\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.startDate").exists());
    }

    @Test
    void recordsADose() throws Exception {
        Instant slot = Instant.parse("2026-01-20T07:00:00Z");
        when(doseService.record(eq(1L), eq(3L), any())).thenReturn(new DoseResponse(
                3L, "Metformin", "500 mg", false, slot, DoseStatus.TAKEN, 11L, slot, null));

        mvc.perform(post("/api/medications/3/doses")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-01-20T07:00:00Z\",\"status\":\"TAKEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logId").value(11))
                .andExpect(jsonPath("$.status").value("TAKEN"));

        verify(doseService).record(1L, 3L, new RecordDoseRequest(slot, DoseStatus.TAKEN, null));
    }

    @Test
    void scheduleForADay() throws Exception {
        when(doseService.forDay(1L, LocalDate.of(2026, 1, 20))).thenReturn(List.of());

        mvc.perform(get("/api/doses").param("date", "2026-01-20").with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adherence() throws Exception {
        when(doseService.adherence(1L, null, null)).thenReturn(new AdherenceResponse(
                LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 20),
                new AdherenceStats(60, 50, 4, 5, 1, 84.7), List.of(), List.of()));

        mvc.perform(get("/api/doses/adherence").with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall.percent").value(84.7))
                .andExpect(jsonPath("$.from").value("2025-12-22"));
    }

    @Test
    void undoRemovesTheLog() throws Exception {
        mvc.perform(delete("/api/doses/11").with(TestAuth.signedIn())).andExpect(status().isNoContent());
        verify(doseService).undo(1L, 11L);
    }
}
