package com.healthtracker.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthtracker.config.SecurityConfig;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Every error, whatever threw it, comes back in the same JSON shape. */
@WebMvcTest(GlobalExceptionHandlerTest.ErrorsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandlerTest.ErrorsController.class})
class GlobalExceptionHandlerTest {

    record Body(@NotBlank String name, @Min(1) int count) {}

    @RestController
    static class ErrorsController {

        @PostMapping("/api/test/body")
        String body(@Valid @RequestBody Body body) {
            return "ok";
        }

        @GetMapping("/api/test/number")
        String number(@RequestParam int value) {
            return "ok";
        }

        @GetMapping("/api/test/conflict")
        String conflict() {
            throw ApiException.conflict("Already there");
        }

        @GetMapping("/api/test/boom")
        String boom() {
            throw new IllegalStateException("database password is hunter2");
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    @Test
    void validationErrorsListTheFields() throws Exception {
        mvc.perform(post("/api/test/body")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"count\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.count").exists());
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mvc.perform(post("/api/test/body")
                        .with(TestAuth.signedIn())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void namesTheParameterThatIsMissingOrWrong() throws Exception {
        mvc.perform(get("/api/test/number").with(TestAuth.signedIn()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing parameter 'value'"));
        mvc.perform(get("/api/test/number?value=lots").with(TestAuth.signedIn()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'value'"));
    }

    @Test
    void apiExceptionsKeepTheirStatusAndCode() throws Exception {
        mvc.perform(get("/api/test/conflict").with(TestAuth.signedIn()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"))
                .andExpect(jsonPath("$.message").value("Already there"));
    }

    @Test
    void unexpectedErrorsDoNotLeakDetails() throws Exception {
        mvc.perform(get("/api/test/boom").with(TestAuth.signedIn()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal_error"))
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    @Test
    void unknownEndpointsAreAJson404() throws Exception {
        mvc.perform(get("/api/test/nothing-here").with(TestAuth.signedIn()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }
}
