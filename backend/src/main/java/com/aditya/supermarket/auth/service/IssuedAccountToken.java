package com.aditya.supermarket.auth.service;

import java.time.Duration;
import java.time.Instant;

/**
 * A freshly created token. {@code rawToken} exists only in memory (it goes into the email) and is
 * never stored; {@link #toString()} hides it so it cannot leak into logs.
 */
public record IssuedAccountToken(String rawToken, Instant expiresAt, Duration validFor) {

    @Override
    public String toString() {
        return "IssuedAccountToken[rawToken=***, expiresAt=" + expiresAt + ", validFor=" + validFor + "]";
    }
}
