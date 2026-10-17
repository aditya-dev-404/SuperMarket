package com.aditya.supermarket.common.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Helpers for reading the authenticated user from the validated JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** The user id is stored in the JWT subject. */
    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
