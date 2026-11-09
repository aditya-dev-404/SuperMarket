package com.aditya.supermarket.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.auth.dto.ChangePasswordRequest;
import com.aditya.supermarket.auth.dto.LoginRequest;
import com.aditya.supermarket.common.exception.EmailNotVerifiedException;
import com.aditya.supermarket.notification.event.PasswordChangedEvent;
import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.service.UserService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Covers the Phase 7 additions to {@link AuthService}: verified-only login and the password-changed alert. */
@ExtendWith(MockitoExtension.class)
class AuthServiceVerificationTest {

    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher events;

    private AuthService service;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(any())).thenReturn("dummy-hash");
        service = new AuthService(userService, jwtService, refreshTokenService, passwordEncoder, events);
    }

    @Test
    void login_rejectsUnverifiedAccountsWithAMachineReadableError() {
        User unverified = user(false);
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(unverified));
        when(passwordEncoder.matches("Passw0rd1", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("a@example.com", "Passw0rd1")))
            .isInstanceOf(EmailNotVerifiedException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void login_withAWrongPassword_doesNotRevealThatTheAccountIsUnverified() {
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(user(false)));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("a@example.com", "wrong")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void changePassword_signsOutEverywhereAndSendsAnAlert() {
        User user = user(true);
        when(userService.getById(7L)).thenReturn(user);

        service.changePassword(7L, new ChangePasswordRequest("OldPassw0rd", "NewPassw0rd"));

        verify(userService).changePassword(7L, "OldPassw0rd", "NewPassw0rd");
        verify(refreshTokenService).revokeAllForUser(7L);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new PasswordChangedEvent(7L, "a@example.com", "Aditya"));
    }

    private static User user(boolean verified) {
        User user = new User("a@example.com", "hash", "Aditya", "Kumar", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 7L);
        if (verified) {
            user.markEmailVerified(Instant.parse("2026-11-04T10:00:00Z"));
        }
        return user;
    }
}
