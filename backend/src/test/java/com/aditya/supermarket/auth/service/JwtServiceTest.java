package com.aditya.supermarket.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aditya.supermarket.auth.config.JwtConfig;
import com.aditya.supermarket.auth.config.JwtProperties;
import com.aditya.supermarket.auth.service.JwtService.AccessToken;
import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String ISSUER = "https://api.supermarket.local";
    private static final String SECRET = "unit-test-secret-that-is-at-least-32-chars-long";

    private final JwtProperties properties =
            new JwtProperties(ISSUER, SECRET, Duration.ofMinutes(15), Duration.ofDays(7));
    private final JwtConfig config = new JwtConfig();
    private final JwtService jwtService =
            new JwtService(config.jwtEncoder(properties), properties, Clock.systemUTC());
    private final JwtDecoder decoder = config.jwtDecoder(properties);

    private User vendorWithId(long id) {
        User user = new User("vendor@example.com", "hash", "Vera", "Vendor", Role.VENDOR);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void tokenCarriesSubjectEmailAndRole() {
        AccessToken token = jwtService.createAccessToken(vendorWithId(42L));

        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("vendor@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("VENDOR");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(token.expiresInSeconds()).isEqualTo(900);
    }

    @Test
    void tamperedTokenIsRejected() {
        AccessToken token = jwtService.createAccessToken(vendorWithId(42L));

        assertThatThrownBy(() -> decoder.decode(token.value() + "x")).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtProperties other = new JwtProperties(
                ISSUER, "another-secret-that-is-also-32-chars-long!", Duration.ofMinutes(15), Duration.ofDays(7));
        JwtService otherService = new JwtService(config.jwtEncoder(other), other, Clock.systemUTC());
        AccessToken forged = otherService.createAccessToken(vendorWithId(1L));

        assertThatThrownBy(() -> decoder.decode(forged.value())).isInstanceOf(JwtException.class);
    }
}
