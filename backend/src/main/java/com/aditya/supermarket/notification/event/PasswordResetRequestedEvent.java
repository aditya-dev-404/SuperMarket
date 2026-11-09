package com.aditya.supermarket.notification.event;

import java.time.Duration;

/** Published after a password-reset token was issued. The raw token is hidden from {@link #toString()}. */
public record PasswordResetRequestedEvent(
    Long userId, String email, String firstName, String token, Duration validFor) {

    @Override
    public String toString() {
        return "PasswordResetRequestedEvent[userId=" + userId + "]";
    }
}
