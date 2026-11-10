package com.aditya.supermarket.auth.job;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.auth.config.AccountProperties;
import com.aditya.supermarket.user.service.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnverifiedAccountCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");

    @Mock
    private UserService userService;

    private UnverifiedAccountCleanupJob job;

    @BeforeEach
    void setUp() {
        AccountProperties properties = new AccountProperties(
            Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofMinutes(1), Duration.ofDays(7));
        job = new UnverifiedAccountCleanupJob(userService, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void deleteAbandonedSignups_usesTheRetentionPeriodAsCutoff() {
        when(userService.deleteUnverifiedCreatedBefore(NOW.minus(Duration.ofDays(7)))).thenReturn(2);

        job.deleteAbandonedSignups();

        verify(userService).deleteUnverifiedCreatedBefore(Instant.parse("2026-11-03T00:00:00Z"));
    }

    @Test
    void deleteAbandonedSignups_neverThrows() {
        doThrow(new IllegalStateException("db down")).when(userService).deleteUnverifiedCreatedBefore(
            Instant.parse("2026-11-03T00:00:00Z"));

        job.deleteAbandonedSignups();
    }
}
