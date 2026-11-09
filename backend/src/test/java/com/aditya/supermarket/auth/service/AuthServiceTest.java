package com.aditya.supermarket.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.auth.dto.LoginRequest;
import com.aditya.supermarket.auth.service.JwtService.AccessToken;
import com.aditya.supermarket.auth.service.RefreshTokenService.IssuedToken;
import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.service.UserService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private ApplicationEventPublisher events;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, jwtService, refreshTokenService, passwordEncoder, events);
    }

    private User customer() {
        User user = new User("jane@example.com", passwordEncoder.encode("Passw0rd1"), "Jane", "Doe", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 5L);
        user.markEmailVerified(Instant.now());
        return user;
    }

    @Test
    void loginWithCorrectPasswordReturnsTokens() {
        User user = customer();
        when(userService.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(user)).thenReturn(new AccessToken("jwt", 900));
        when(refreshTokenService.issue(5L)).thenReturn(new IssuedToken("refresh", Instant.parse("2026-01-08T00:00:00Z")));

        AuthResult result = authService.login(new LoginRequest("jane@example.com", "Passw0rd1"));

        assertThat(result.response().accessToken()).isEqualTo("jwt");
        assertThat(result.response().tokenType()).isEqualTo("Bearer");
        assertThat(result.response().user().email()).isEqualTo("jane@example.com");
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void loginWithWrongPasswordFails() {
        when(userService.findByEmail("jane@example.com")).thenReturn(Optional.of(customer()));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong-pass1")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginWithUnknownEmailGivesTheSameError() {
        when(userService.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "Passw0rd1")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
