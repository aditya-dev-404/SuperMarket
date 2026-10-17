package com.aditya.supermarket.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings bound from {@code app.jwt.*}.
 *
 * @param issuer          value of the {@code iss} claim, also validated on every request
 * @param secret          HMAC signing key, at least 32 characters (HS256)
 * @param accessTokenTtl  lifetime of access tokens
 * @param refreshTokenTtl lifetime of refresh tokens
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank @Size(min = 32, message = "app.jwt.secret must be at least 32 characters") String secret,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl) {
}
