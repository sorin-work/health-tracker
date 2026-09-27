package com.healthtracker.importing;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthtracker.config.SecurityConfig;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(ImportController.class)
@Import(SecurityConfig.class)
class ImportControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VitalImportService vitalImportService;

    @MockitoBean
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    @Test
    void passesTheFileThrough() throws Exception {
        String csv = "measured_at,type,value\n2026-09-01 07:30,WEIGHT,82.4\n";
        when(vitalImportService.importCsv(1L, csv, true))
                .thenReturn(new ImportResult(true, 1, 0, 0, List.of(), List.of()));

        mvc.perform(multipart("/api/import/vitals")
                        .file(new MockMultipartFile("file", "vitals.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                        .param("dryRun", "true")
                        .with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.rows").value(1));

        verify(vitalImportService).importCsv(eq(1L), eq(csv), eq(true));
    }

    @Test
    void rejectsAnEmptyFile() throws Exception {
        mvc.perform(multipart("/api/import/vitals")
                        .file(new MockMultipartFile("file", "vitals.csv", "text/csv", new byte[0]))
                        .with(TestAuth.signedIn()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The file is empty"));
        verifyNoInteractions(vitalImportService);
    }

    @Test
    void needsAFile() throws Exception {
        mvc.perform(multipart("/api/import/vitals").with(TestAuth.signedIn()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing file 'file'"));
    }

    @Test
    void tooLargeFilesGetAJson413() throws Exception {
        when(vitalImportService.importCsv(anyLong(), anyString(), anyBoolean()))
                .thenThrow(new MaxUploadSizeExceededException(5 * 1024 * 1024));

        mvc.perform(multipart("/api/import/vitals")
                        .file(new MockMultipartFile("file", "v.csv", "text/csv", "x".getBytes(StandardCharsets.UTF_8)))
                        .with(TestAuth.signedIn()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("too_large"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(multipart("/api/import/vitals")
                        .file(new MockMultipartFile("file", "v.csv", "text/csv", "x".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(vitalImportService);
    }
}
