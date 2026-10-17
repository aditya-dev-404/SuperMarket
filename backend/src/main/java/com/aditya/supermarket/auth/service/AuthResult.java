package com.aditya.supermarket.auth.service;

import com.aditya.supermarket.auth.dto.AuthResponse;
import java.time.Instant;

/**
 * Outcome of a successful authentication: the JSON body for the client plus the refresh token
 * the controller must put into the HttpOnly cookie.
 */
public record AuthResult(AuthResponse response, String refreshToken, Instant refreshTokenExpiresAt) {
}
