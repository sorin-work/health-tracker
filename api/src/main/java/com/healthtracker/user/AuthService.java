package com.healthtracker.user;

import com.healthtracker.common.ApiException;
import com.healthtracker.security.JwtService;
import com.healthtracker.user.dto.AuthResponse;
import com.healthtracker.user.dto.LoginRequest;
import com.healthtracker.user.dto.RegisterRequest;
import com.healthtracker.user.dto.UserResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        String timezone = Timezones.orDefault(request.timezone());
        User user = users.save(new User(
                email, passwordEncoder.encode(request.password()), request.displayName().trim(), timezone, clock.instant()));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "invalid_credentials", "Email or password is incorrect"));
        return tokenFor(user);
    }

    private AuthResponse tokenFor(User user) {
        Instant now = clock.instant();
        String token = jwtService.issue(user.getId(), user.getEmail());
        return new AuthResponse(token, jwtService.expiresAt(now), UserResponse.from(user));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
