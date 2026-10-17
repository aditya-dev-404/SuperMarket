package com.aditya.supermarket.auth.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Refresh token cookie settings bound from {@code app.auth.refresh-cookie.*}. */
@Validated
@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
public record RefreshCookieProperties(
        @NotBlank String name,
        @NotBlank String path,
        @NotBlank String sameSite,
        boolean secure) {
}
