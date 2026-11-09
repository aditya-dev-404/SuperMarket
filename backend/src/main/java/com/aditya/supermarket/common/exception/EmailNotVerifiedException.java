package com.aditya.supermarket.common.exception;

/** Thrown when correct credentials belong to an account whose email was never confirmed (maps to HTTP 403). */
public class EmailNotVerifiedException extends RuntimeException {

    public static final String CODE = "EMAIL_NOT_VERIFIED";

    public EmailNotVerifiedException() {
        super("Please verify your email address before logging in. Check your inbox for the verification link.");
    }
}
