package com.aditya.supermarket.auth.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for email verification and password reset ({@code app.account.*}). All have defaults,
 * so nothing has to be added to application.properties unless you want to change them.
 *
 * @param verificationTokenTtl how long an email-verification link stays valid
 * @param passwordResetTokenTtl how long a password-reset link stays valid
 * @param emailCooldown minimum time between two verification / reset emails for the same user
 * @param unverifiedRetention unverified accounts older than this are deleted by the cleanup job
 */
@Validated
@ConfigurationProperties(prefix = "app.account")
public record AccountProperties(
    @DefaultValue("PT24H") @NotNull Duration verificationTokenTtl,
    @DefaultValue("PT30M") @NotNull Duration passwordResetTokenTtl,
    @DefaultValue("PT1M") @NotNull Duration emailCooldown,
    @DefaultValue("P7D") @NotNull Duration unverifiedRetention) {
}
