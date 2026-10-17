package com.aditya.supermarket.auth.security;

import com.aditya.supermarket.auth.config.RefreshCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/**
 * Builds and reads the refresh token cookie. The cookie is HttpOnly (invisible to JavaScript),
 * scoped to the auth endpoints and SameSite=Strict.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieManager {

    private final RefreshCookieProperties properties;
    private final Clock clock;

    public ResponseCookie create(String tokenValue, Instant expiresAt) {
        return build(tokenValue, Duration.between(Instant.now(clock), expiresAt));
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, properties.name());
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(cookie.getValue());
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path())
                .maxAge(maxAge)
                .build();
    }
}
