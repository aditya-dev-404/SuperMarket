package com.aditya.supermarket.notification.entity;

import static com.aditya.supermarket.notification.NotificationTestData.NOW;
import static com.aditya.supermarket.notification.NotificationTestData.pendingLog;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class NotificationLogTest {

    private static final Duration DELAY = Duration.ofMinutes(2);

    @Test
    void pending_startsWithZeroAttemptsAndKeepsBodies() {
        NotificationLog entry = pendingLog(1L);

        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(entry.getAttempts()).isZero();
        assertThat(entry.getHtmlBody()).isNotNull();
        assertThat(entry.getTextBody()).isNotNull();
    }

    @Test
    void markSent_clearsBodiesAndSchedule() {
        NotificationLog entry = pendingLog(1L);

        entry.markSent(NOW);

        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(entry.getAttempts()).isEqualTo(1);
        assertThat(entry.getSentAt()).isEqualTo(NOW);
        assertThat(entry.getNextAttemptAt()).isNull();
        assertThat(entry.getHtmlBody()).isNull();
        assertThat(entry.getTextBody()).isNull();
    }

    @Test
    void markAttemptFailed_staysPendingWithLinearBackoff_belowMaxAttempts() {
        NotificationLog entry = pendingLog(1L);

        entry.markAttemptFailed("smtp down", NOW, 3, DELAY);
        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(entry.getNextAttemptAt()).isEqualTo(NOW.plus(DELAY));

        entry.markAttemptFailed("smtp down", NOW, 3, DELAY);
        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(entry.getNextAttemptAt()).isEqualTo(NOW.plus(DELAY.multipliedBy(2)));
        assertThat(entry.getHtmlBody()).isNotNull();
    }

    @Test
    void markAttemptFailed_givesUpAndClearsBodies_atMaxAttempts() {
        NotificationLog entry = pendingLog(1L);

        entry.markAttemptFailed("smtp down", NOW, 2, DELAY);
        entry.markAttemptFailed("smtp still down", NOW, 2, DELAY);

        assertThat(entry.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(entry.getAttempts()).isEqualTo(2);
        assertThat(entry.getNextAttemptAt()).isNull();
        assertThat(entry.getLastError()).isEqualTo("smtp still down");
        assertThat(entry.getHtmlBody()).isNull();
        assertThat(entry.getTextBody()).isNull();
    }

    @Test
    void markAttemptFailed_truncatesVeryLongErrors() {
        NotificationLog entry = pendingLog(1L);

        entry.markAttemptFailed("x".repeat(2_000), NOW, 3, DELAY);

        assertThat(entry.getLastError()).hasSize(500);
    }
}
