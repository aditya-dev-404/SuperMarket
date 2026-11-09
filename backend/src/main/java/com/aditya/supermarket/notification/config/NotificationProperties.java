package com.aditya.supermarket.notification.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for transactional emails ({@code app.notification.*}).
 *
 * @param enabled         master switch; when false nothing is rendered, stored or sent
 * @param fromAddress     the single application mailbox every email is sent from
 * @param fromName        display name shown next to the address
 * @param frontendBaseUrl base URL used to build links inside emails (no trailing slash needed)
 * @param maxAttempts     total delivery attempts (first try included) before an email is marked FAILED
 * @param retryDelay      base delay before a retry; multiplied by the attempt number (linear back-off)
 * @param retryInterval   how often the retry job looks for due emails
 * @param retryBatchSize  maximum emails the retry job handles per run
 */
@Validated
@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(
    @DefaultValue("true") boolean enabled,
    @NotBlank String fromAddress,
    @DefaultValue("superMarket") @NotBlank String fromName,
    @NotBlank String frontendBaseUrl,
    @DefaultValue("3") @Min(1) int maxAttempts,
    @DefaultValue("PT2M") @NotNull Duration retryDelay,
    @DefaultValue("PT1M") @NotNull Duration retryInterval,
    @DefaultValue("20") @Min(1) int retryBatchSize) {

    /** Builds an absolute link for the web app, e.g. {@code link("/verify-email?token=abc")}. */
    public String link(String path) {
        String base = frontendBaseUrl.endsWith("/")
            ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
            : frontendBaseUrl;
        String suffix = path.startsWith("/") ? path : "/" + path;
        return base + suffix;
    }
}
