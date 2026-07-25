package com.healthtracker.export;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthtracker.config.SecurityConfig;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExportController.class)
@Import(SecurityConfig.class)
class ExportControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ExportService exportService;

    @MockitoBean
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    @Test
    void downloadsCsvAsAnAttachment() throws Exception {
        when(exportService.export(1L, ExportDataset.VITALS, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(new ExportService.Export(
                        "health-tracker-vitals-2026-01-01_2026-03-31.csv", "measured_at,type\r\n"));

        mvc.perform(get("/api/export/vitals.csv")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31")
                        .with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"health-tracker-vitals-2026-01-01_2026-03-31.csv\""))
                .andExpect(content().string("measured_at,type\r\n"));
    }

    @Test
    void unknownDatasetIs404() throws Exception {
        mvc.perform(get("/api/export/passwords.csv").with(TestAuth.signedIn())).andExpect(status().isNotFound());
    }

    @Test
    void exportNeedsAuthentication() throws Exception {
        mvc.perform(get("/api/export/vitals.csv")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"mood.csv,MOOD", "water.csv,WATER", "journal.csv,JOURNAL", "Doses.csv,DOSES"})
    void everyDatasetHasAUrl(String path, ExportDataset dataset) throws Exception {
        when(exportService.export(1L, dataset, null, null))
                .thenReturn(new ExportService.Export("health-tracker-export.csv", "a\r\n"));

        mvc.perform(get("/api/export/" + path).with(TestAuth.signedIn())).andExpect(status().isOk());
    }
}
