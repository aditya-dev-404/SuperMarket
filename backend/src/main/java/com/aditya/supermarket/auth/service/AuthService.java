package com.aditya.supermarket.auth.service;

import com.aditya.supermarket.auth.dto.AuthResponse;
import com.aditya.supermarket.auth.dto.ChangePasswordRequest;
import com.aditya.supermarket.auth.dto.LoginRequest;
import com.aditya.supermarket.auth.service.JwtService.AccessToken;
import com.aditya.supermarket.auth.service.RefreshTokenService.IssuedToken;
import com.aditya.supermarket.common.exception.EmailNotVerifiedException;
import com.aditya.supermarket.notification.event.PasswordChangedEvent;
import com.aditya.supermarket.user.dto.UserResponse;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.service.UserService;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login, token refresh/rotation, logout and password change. Sign-up lives in {@link AccountService}. */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final String INVALID_REFRESH_TOKEN = "Invalid or expired refresh token";

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    /** Compared against when the email is unknown, so response time does not reveal which emails exist. */
    private final String dummyPasswordHash;

    public AuthService(UserService userService, JwtService jwtService,
                       RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher events) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-password-1");
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        Optional<User> found = userService.findByEmail(request.email());
        String hashToCheck = found.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
        if (found.isEmpty() || !passwordMatches || !found.get().isEnabled()) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        // Checked only after the password matched, so this message never reveals whether an email exists.
        if (!found.get().isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        return issueTokens(found.get());
    }

    /** {@code noRollbackFor} so that reuse detection in the token service is committed. */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResult refresh(String rawRefreshToken) {
        Long userId = refreshTokenService.consume(rawRefreshToken);
        User user = userService.findById(userId)
            .filter(User::isEnabled)
            .orElseThrow(() -> new BadCredentialsException(INVALID_REFRESH_TOKEN));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    /** Changes the password and signs the user out of every device. */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
        refreshTokenService.revokeAllForUser(userId);
        User user = userService.getById(userId);
        events.publishEvent(new PasswordChangedEvent(user.getId(), user.getEmail(), user.getFirstName()));
    }

    private AuthResult issueTokens(User user) {
        AccessToken accessToken = jwtService.createAccessToken(user);
        IssuedToken refreshToken = refreshTokenService.issue(user.getId());
        AuthResponse response = new AuthResponse(
            accessToken.value(), "Bearer", accessToken.expiresInSeconds(), UserResponse.from(user));
        return new AuthResult(response, refreshToken.value(), refreshToken.expiresAt());
    }
}
