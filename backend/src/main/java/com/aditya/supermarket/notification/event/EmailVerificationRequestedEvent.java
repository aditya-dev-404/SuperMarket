package com.aditya.supermarket.notification.event;

import java.time.Duration;

/**
 * Published after a verification token was issued. Carries the raw token because it must go into
 * the email; {@link #toString()} hides it.
 */
public record EmailVerificationRequestedEvent(
    Long userId, String email, String firstName, String token, Duration validFor) {

    @Override
    public String toString() {
        return "EmailVerificationRequestedEvent[userId=" + userId + "]";
    }
}
