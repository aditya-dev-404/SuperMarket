package com.aditya.supermarket.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional bootstrap admin account bound from {@code app.admin.*}.
 * Both values come from the ADMIN_EMAIL / ADMIN_PASSWORD environment variables.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
