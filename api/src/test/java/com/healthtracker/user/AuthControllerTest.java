package com.healthtracker.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthtracker.common.ApiException;
import com.healthtracker.config.SecurityConfig;
import com.healthtracker.security.JwtService;
import com.healthtracker.support.TestAuth;
import com.healthtracker.user.dto.AuthResponse;
import com.healthtracker.user.dto.UserResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, MeController.class})
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtService jwtService;

    private final UserResponse sam = new UserResponse(1L, "sam@example.com", "Sam", "UTC", Instant.EPOCH);

    @BeforeEach
    void setUp() {
        TestAuth.acceptTestToken(jwtService);
    }

    @Test
    void registerReturnsATokenWithoutBeingSignedIn() throws Exception {
        when(authService.register(any())).thenReturn(new AuthResponse("jwt", Instant.EPOCH, sam));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sam@example.com","password":"password1","displayName":"Sam"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.user.email").value("sam@example.com"));
    }

    @Test
    void registerValidatesTheBody() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","displayName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").value("must be at least 8 characters"))
                .andExpect(jsonPath("$.fields.displayName").exists());
    }

    @Test
    void badCredentialsAre401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Email or password is incorrect"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sam@example.com\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void meNeedsAToken() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void meRejectsAnInvalidToken() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Bearer forged")).andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsTheProfile() throws Exception {
        User user = new User("sam@example.com", "hash", "Sam", "Europe/Berlin", Instant.EPOCH);
        user.setId(1L);
        when(userService.get(1L)).thenReturn(user);

        mvc.perform(get("/api/me").with(TestAuth.signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Sam"))
                .andExpect(jsonPath("$.timezone").value("Europe/Berlin"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
