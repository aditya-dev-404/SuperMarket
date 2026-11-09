package com.aditya.supermarket.notification.entity;

/** Delivery state of a logged email. */
public enum NotificationStatus {
    /** Waiting for (another) delivery attempt. */
    PENDING,
    /** Accepted by the SMTP server. */
    SENT,
    /** Gave up after the maximum number of attempts. */
    FAILED
}
