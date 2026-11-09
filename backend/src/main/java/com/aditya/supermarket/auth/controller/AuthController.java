package com.aditya.supermarket.auth.controller;

import com.aditya.supermarket.auth.dto.AuthResponse;
import com.aditya.supermarket.auth.dto.ChangePasswordRequest;
import com.aditya.supermarket.auth.dto.LoginRequest;
import com.aditya.supermarket.auth.dto.MessageResponse;
import com.aditya.supermarket.auth.dto.RegisterRequest;
import com.aditya.supermarket.auth.security.RefreshCookieManager;
import com.aditya.supermarket.auth.service.AccountService;
import com.aditya.supermarket.auth.service.AuthResult;
import com.aditya.supermarket.auth.service.AuthService;
import com.aditya.supermarket.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints. Access token in the body, refresh token in an HttpOnly cookie. */
@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    static final String REGISTERED_MESSAGE =
        "Account created. Check your inbox and click the link to verify your email before logging in.";

    private final AuthService authService;
    private final AccountService accountService;
    private final RefreshCookieManager cookieManager;

    @Operation(summary = "Register a new customer account (not logged in until the email is verified)")
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        accountService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MessageResponse(REGISTERED_MESSAGE));
    }

    @Operation(summary = "Log in with email and password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return respond(HttpStatus.OK, authService.login(request));
    }

    @Operation(summary = "Get a new access token using the refresh cookie (rotates the refresh token)")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String refreshToken = cookieManager.read(request)
            .orElseThrow(() -> new BadCredentialsException("Missing refresh token"));
        return respond(HttpStatus.OK, authService.refresh(refreshToken));
    }

    @Operation(summary = "Log out: revokes the refresh token and clears the cookie")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        cookieManager.read(request).ifPresent(authService::logout);
        return clearedCookieResponse();
    }

    @Operation(summary = "Change my password (signs out all devices)")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.id(jwt), request);
        return clearedCookieResponse();
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus status, AuthResult result) {
        String cookie = cookieManager.create(result.refreshToken(), result.refreshTokenExpiresAt()).toString();
        return ResponseEntity.status(status).header(HttpHeaders.SET_COOKIE, cookie).body(result.response());
    }

    private ResponseEntity<Void> clearedCookieResponse() {
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookieManager.clear().toString())
            .build();
    }
}
