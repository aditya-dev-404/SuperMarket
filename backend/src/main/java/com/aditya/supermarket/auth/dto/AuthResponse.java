package com.aditya.supermarket.auth.dto;

import com.aditya.supermarket.user.dto.UserResponse;

/**
 * Body returned by register, login and refresh. The refresh token is NOT in the body:
 * it travels only in an HttpOnly cookie.
 *
 * @param expiresIn access token lifetime in seconds
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
