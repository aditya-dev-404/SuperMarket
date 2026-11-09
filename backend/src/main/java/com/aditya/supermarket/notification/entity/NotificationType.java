package com.aditya.supermarket.notification.entity;

/** Every kind of transactional email the application sends. Stored as text in notification_log. */
public enum NotificationType {
    WELCOME,
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    PASSWORD_CHANGED,
    VENDOR_APPLICATION_RECEIVED,
    VENDOR_APPLICATION_ADMIN_ALERT,
    VENDOR_APPROVED,
    VENDOR_REJECTED,
    ORDER_CONFIRMATION,
    VENDOR_NEW_ORDER,
    PAYMENT_FAILED,
    ORDER_CANCELLED,
    VENDOR_ORDER_CANCELLED,
    ORDER_SHIPPED,
    ORDER_DELIVERED
}
