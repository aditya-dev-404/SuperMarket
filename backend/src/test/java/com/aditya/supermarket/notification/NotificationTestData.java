package com.aditya.supermarket.notification;

import com.aditya.supermarket.notification.config.NotificationProperties;
import com.aditya.supermarket.notification.dto.NotificationRequest;
import com.aditya.supermarket.notification.entity.NotificationLog;
import com.aditya.supermarket.notification.entity.NotificationType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;

/** Builders shared by the notification tests. */
public final class NotificationTestData {

    public static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private NotificationTestData() {
    }

    public static NotificationProperties properties() {
        return properties(true, 3);
    }

    public static NotificationProperties properties(boolean enabled, int maxAttempts) {
        return new NotificationProperties(enabled, "shop@example.com", "superMarket",
            "http://localhost:5173", maxAttempts, Duration.ofMinutes(2), Duration.ofMinutes(1), 20);
    }

    public static NotificationRequest request() {
        return new NotificationRequest(NotificationType.PASSWORD_CHANGED, "aditya@example.com",
            "Your password was changed", "generic",
            Map.of("heading", "Password changed", "paragraphs", List.of("Your password was changed.")),
            "42");
    }

    public static NotificationLog pendingLog(long id) {
        NotificationLog entry = NotificationLog.pending(NotificationType.PASSWORD_CHANGED,
            "aditya@example.com", "Your password was changed", "<p>html</p>", "text", "42",
            NOW.plus(Duration.ofMinutes(2)));
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }
}
