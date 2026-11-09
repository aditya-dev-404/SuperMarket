package com.aditya.supermarket.auth.job;

import com.aditya.supermarket.auth.config.AccountProperties;
import com.aditya.supermarket.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Removes sign-ups that never confirmed their email, so abandoned accounts do not pile up. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnverifiedAccountCleanupJob {

    private final UserService userService;
    private final AccountProperties properties;
    private final Clock clock;

    /** Runs every day at 03:30 server time. */
    @Scheduled(cron = "0 30 3 * * *")
    public void deleteAbandonedSignups() {
        Instant cutoff = clock.instant().minus(properties.unverifiedRetention());
        try {
            int deleted = userService.deleteUnverifiedCreatedBefore(cutoff);
            if (deleted > 0) {
                log.info("Deleted {} unverified account(s) older than {}", deleted, properties.unverifiedRetention());
            }
        } catch (RuntimeException e) {
            // A failed cleanup must never affect the application; it simply runs again tomorrow.
            log.error("Cleanup of unverified accounts failed", e);
        }
    }
}
