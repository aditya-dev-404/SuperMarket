package com.aditya.supermarket.notification.event;

/** Published after a password was changed or reset (security alert to the owner). */
public record PasswordChangedEvent(Long userId, String email, String firstName) {
}
