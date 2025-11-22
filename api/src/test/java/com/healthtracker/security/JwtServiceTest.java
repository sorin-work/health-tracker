package com.healthtracker.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.healthtracker.config.JwtProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static JwtService at(Instant now, String secret) {
        return new JwtService(new JwtProperties(secret, Duration.ofHours(1)), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void issuedTokensVerify() {
        JwtService jwt = at(NOW, SECRET);

        String token = jwt.issue(42L, "sam@example.com");

        assertThat(jwt.verify(token)).contains(new AuthUser(42L, "sam@example.com"));
    }

    @Test
    void expiredTokensAreRejected() {
        String token = at(NOW, SECRET).issue(42L, "sam@example.com");

        assertThat(at(NOW.plus(Duration.ofHours(2)), SECRET).verify(token)).isEmpty();
    }

    @Test
    void tokensSignedWithAnotherKeyAreRejected() {
        String token = at(NOW, "another-secret-that-is-also-32-bytes-long!").issue(42L, "sam@example.com");

        assertThat(at(NOW, SECRET).verify(token)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(at(NOW, SECRET).verify("not-a-jwt")).isEmpty();
    }

    @Test
    void refusesShortSecrets() {
        assertThatThrownBy(() -> at(NOW, "short")).isInstanceOf(IllegalStateException.class);
    }
}
