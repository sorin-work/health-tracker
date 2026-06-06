package com.healthtracker.water;

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

import com.healthtracker.config.SecurityConfig;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import com.healthtracker.water.dto.WaterDay;
import com.healthtracker.water.dto.WaterEntry;
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

@WebMvcTest(WaterController.class)
@Import(SecurityConfig.class)
class WaterControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    WaterService waterService;

    @MockitoBean
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    @Test
    void returnsTodaysTotal() throws Exception {
        Instant at = Instant.parse("2026-06-10T08:00:00Z");
        when(waterService.day(1L, null))
                .thenReturn(new WaterDay(LocalDate.of(2026, 6, 10), 250, List.of(new WaterEntry(3L, 250, at))));

        mvc.perform(get("/api/water").with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMl").value(250))
                .andExpect(jsonPath("$.entries[0].id").value(3));
    }

    @Test
    void addsAGlass() throws Exception {
        when(waterService.add(eq(1L), any()))
                .thenReturn(new WaterEntry(4L, 330, Instant.parse("2026-06-10T08:00:00Z")));

        mvc.perform(post("/api/water")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMl\":330}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountMl").value(330));
    }

    @Test
    void rejectsAnEmptyGlass() throws Exception {
        mvc.perform(post("/api/water")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMl\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amountMl").exists());
        verifyNoInteractions(waterService);
    }

    @Test
    void deletesAnEntry() throws Exception {
        mvc.perform(delete("/api/water/4").with(TestAuth.signedIn())).andExpect(status().isNoContent());

        verify(waterService).delete(1L, 4L);
    }
}
