package com.aditya.supermarket.notification.event;

/** Published when a user confirmed their email address; triggers the welcome email. */
public record EmailVerifiedEvent(Long userId, String email, String firstName) {
}
