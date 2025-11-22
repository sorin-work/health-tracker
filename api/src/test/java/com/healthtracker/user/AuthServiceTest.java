package com.healthtracker.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthtracker.common.ApiException;
import com.healthtracker.config.JwtProperties;
import com.healthtracker.security.JwtService;
import com.healthtracker.user.dto.AuthResponse;
import com.healthtracker.user.dto.LoginRequest;
import com.healthtracker.user.dto.RegisterRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T09:00:00Z");

    @Mock
    UserRepository users;

    PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    JwtService jwt;
    AuthService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        jwt = new JwtService(new JwtProperties("test-secret-that-is-at-least-32-bytes-long", Duration.ofDays(7)), clock);
        service = new AuthService(users, encoder, jwt, clock);
    }

    @Test
    void registerNormalisesEmailAndHashesPassword() {
        when(users.existsByEmailIgnoreCase("sam@example.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });

        AuthResponse response = service.register(
                new RegisterRequest("  Sam@Example.com ", "correct horse", " Sam ", "Europe/Berlin"));

        assertThat(response.user().email()).isEqualTo("sam@example.com");
        assertThat(response.user().displayName()).isEqualTo("Sam");
        assertThat(response.user().timezone()).isEqualTo("Europe/Berlin");
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(jwt.verify(response.token())).hasValueSatisfying(u -> assertThat(u.id()).isEqualTo(5L));
    }

    @Test
    void registerDefaultsToUtc() {
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(6L);
            return u;
        });

        AuthResponse response = service.register(new RegisterRequest("a@b.co", "password1", "A", null));

        assertThat(response.user().timezone()).isEqualTo("UTC");
    }

    @Test
    void registerRejectsUnknownTimeZones() {
        assertThatThrownBy(() -> service.register(new RegisterRequest("a@b.co", "password1", "A", "Mars/Olympus")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("time zone");
        verify(users, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateEmails() {
        when(users.existsByEmailIgnoreCase("sam@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("sam@example.com", "password1", "Sam", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void loginWithTheRightPassword() {
        User user = new User("sam@example.com", encoder.encode("password1"), "Sam", "UTC", NOW);
        user.setId(5L);
        when(users.findByEmailIgnoreCase("sam@example.com")).thenReturn(Optional.of(user));

        assertThat(service.login(new LoginRequest("SAM@example.com", "password1")).token()).isNotBlank();
    }

    @Test
    void loginWithTheWrongPasswordIsUnauthorized() {
        User user = new User("sam@example.com", encoder.encode("password1"), "Sam", "UTC", NOW);
        when(users.findByEmailIgnoreCase("sam@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("sam@example.com", "nope")))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginForUnknownEmailLooksTheSame() {
        when(users.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@example.com", "whatever")))
                .hasMessage("Email or password is incorrect");
    }
}
