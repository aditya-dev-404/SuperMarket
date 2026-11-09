package com.aditya.supermarket.notification.exception;

/** The SMTP server could not accept the email right now. This is the only retryable failure. */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
