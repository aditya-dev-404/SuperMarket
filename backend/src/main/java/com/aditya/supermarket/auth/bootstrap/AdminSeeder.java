package com.aditya.supermarket.auth.bootstrap;

import com.aditya.supermarket.auth.config.AdminProperties;
import com.aditya.supermarket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account at startup from the ADMIN_EMAIL / ADMIN_PASSWORD environment
 * variables, so no admin credentials are ever stored in code or migrations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 12;

    private final AdminProperties adminProperties;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (!adminProperties.isConfigured()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set: skipping admin bootstrap");
            return;
        }
        if (adminProperties.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        boolean created = userService.createAdminIfAbsent(adminProperties.email(), adminProperties.password());
        if (created) {
            log.info("Admin account created for {}", adminProperties.email());
        } else {
            log.info("Admin account already exists, nothing to do");
        }
    }
}
