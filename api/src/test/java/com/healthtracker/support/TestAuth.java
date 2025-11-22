package com.healthtracker.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.healthtracker.security.AuthUser;
import com.healthtracker.security.JwtService;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Lets controller tests go through the real JWT filter with a mocked {@link JwtService}. */
public final class TestAuth {

    public static final AuthUser USER = new AuthUser(1L, "sam@example.com");
    private static final String TOKEN = "test-token";

    private TestAuth() {}

    public static void acceptTestToken(JwtService jwtService) {
        lenient().when(jwtService.verify(anyString())).thenReturn(Optional.empty());
        lenient().when(jwtService.verify(TOKEN)).thenReturn(Optional.of(USER));
    }

    public static RequestPostProcessor signedIn() {
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
            return request;
        };
    }
}
