package com.aditya.supermarket.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.auth.dto.RegisterRequest;
import com.aditya.supermarket.auth.entity.AccountTokenType;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.common.exception.DuplicateResourceException;
import com.aditya.supermarket.notification.event.EmailVerificationRequestedEvent;
import com.aditya.supermarket.notification.event.EmailVerifiedEvent;
import com.aditya.supermarket.notification.event.PasswordChangedEvent;
import com.aditya.supermarket.notification.event.PasswordResetRequestedEvent;
import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.service.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final IssuedAccountToken TOKEN =
        new IssuedAccountToken("raw-token", NOW.plusSeconds(60), Duration.ofMinutes(30));
    private static final RegisterRequest REGISTER =
        new RegisterRequest("Aditya@Example.com", "Passw0rd1", "Aditya", "Kumar");

    @Mock
    private UserService userService;
    @Mock
    private AccountTokenService tokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private ApplicationEventPublisher events;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(userService, tokenService, refreshTokenService, events,
            Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void register_createsAccountAndSendsVerificationEmail() {
        User created = user(7L, false);
        when(userService.findByEmail(REGISTER.email())).thenReturn(Optional.empty());
        when(userService.registerCustomer("Aditya@Example.com", "Passw0rd1", "Aditya", "Kumar")).thenReturn(created);
        when(tokenService.issue(7L, AccountTokenType.EMAIL_VERIFICATION)).thenReturn(TOKEN);

        service.register(REGISTER);

        EmailVerificationRequestedEvent event = captured(EmailVerificationRequestedEvent.class);
        assertThat(event.userId()).isEqualTo(7L);
        assertThat(event.email()).isEqualTo("aditya@example.com");
        assertThat(event.token()).isEqualTo("raw-token");
    }

    @Test
    void register_forAnUnverifiedAddress_changesNothingAndOnlyResendsTheLink() {
        User pending = user(7L, false);
        when(userService.findByEmail(REGISTER.email())).thenReturn(Optional.of(pending));
        when(tokenService.issueUnlessRecentlyIssued(7L, AccountTokenType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(TOKEN));

        service.register(REGISTER);

        verify(userService, never()).registerCustomer(anyString(), anyString(), anyString(), anyString());
        captured(EmailVerificationRequestedEvent.class);
    }

    @Test
    void register_forAnUnverifiedAddress_sendsNothingDuringCooldown() {
        when(userService.findByEmail(REGISTER.email())).thenReturn(Optional.of(user(7L, false)));
        when(tokenService.issueUnlessRecentlyIssued(7L, AccountTokenType.EMAIL_VERIFICATION))
            .thenReturn(Optional.empty());

        service.register(REGISTER);

        verifyNoInteractions(events);
    }

    @Test
    void register_rejectsAVerifiedAddress() {
        when(userService.findByEmail(REGISTER.email())).thenReturn(Optional.of(user(7L, true)));
        when(userService.registerCustomer(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new DuplicateResourceException("An account with this email already exists"));

        assertThatThrownBy(() -> service.register(REGISTER)).isInstanceOf(DuplicateResourceException.class);

        verifyNoInteractions(events);
    }

    @Test
    void verifyEmail_marksTheAccountVerifiedAndSendsTheWelcomeEmail() {
        User user = user(7L, false);
        when(tokenService.redeem("raw-token", AccountTokenType.EMAIL_VERIFICATION)).thenReturn(7L);
        when(userService.getById(7L)).thenReturn(user);

        service.verifyEmail("raw-token");

        verify(userService).markEmailVerified(7L, NOW);
        assertThat(captured(EmailVerifiedEvent.class).userId()).isEqualTo(7L);
    }

    @Test
    void verifyEmail_isIdempotentForAnAlreadyVerifiedAccount() {
        when(tokenService.redeem("raw-token", AccountTokenType.EMAIL_VERIFICATION)).thenReturn(7L);
        when(userService.getById(7L)).thenReturn(user(7L, true));

        service.verifyEmail("raw-token");

        verify(userService, never()).markEmailVerified(any(), any());
        verifyNoInteractions(events);
    }

    @Test
    void verifyEmail_propagatesAnInvalidToken() {
        when(tokenService.redeem("bad", AccountTokenType.EMAIL_VERIFICATION))
            .thenThrow(new BusinessRuleException("invalid"));

        assertThatThrownBy(() -> service.verifyEmail("bad")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void resendVerification_sendsOnlyForUnverifiedAccounts() {
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(user(7L, false)));
        when(tokenService.issueUnlessRecentlyIssued(7L, AccountTokenType.EMAIL_VERIFICATION))
            .thenReturn(Optional.of(TOKEN));

        service.resendVerification("a@example.com");

        captured(EmailVerificationRequestedEvent.class);
    }

    @Test
    void resendVerification_doesNothingForVerifiedOrUnknownAddresses() {
        when(userService.findByEmail("verified@example.com")).thenReturn(Optional.of(user(7L, true)));
        when(userService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.resendVerification("verified@example.com");
        service.resendVerification("unknown@example.com");

        verifyNoInteractions(tokenService, events);
    }

    @Test
    void requestPasswordReset_sendsTheResetLink() {
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(user(7L, true)));
        when(tokenService.issueUnlessRecentlyIssued(7L, AccountTokenType.PASSWORD_RESET))
            .thenReturn(Optional.of(TOKEN));

        service.requestPasswordReset("a@example.com");

        PasswordResetRequestedEvent event = captured(PasswordResetRequestedEvent.class);
        assertThat(event.token()).isEqualTo("raw-token");
        assertThat(event.validFor()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void requestPasswordReset_doesNothingForUnknownAddresses() {
        when(userService.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("unknown@example.com");

        verifyNoInteractions(tokenService, events);
    }

    @Test
    void requestPasswordReset_sendsNothingDuringCooldown() {
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(user(7L, true)));
        when(tokenService.issueUnlessRecentlyIssued(7L, AccountTokenType.PASSWORD_RESET))
            .thenReturn(Optional.empty());

        service.requestPasswordReset("a@example.com");

        verifyNoInteractions(events);
    }

    @Test
    void resetPassword_setsPasswordSignsOutEverywhereAndSendsAnAlert() {
        User user = user(7L, true);
        when(tokenService.redeem("raw-token", AccountTokenType.PASSWORD_RESET)).thenReturn(7L);
        when(userService.resetPassword(7L, "NewPassw0rd", NOW)).thenReturn(user);

        service.resetPassword("raw-token", "NewPassw0rd");

        verify(refreshTokenService).revokeAllForUser(7L);
        assertThat(captured(PasswordChangedEvent.class).email()).isEqualTo("aditya@example.com");
    }

    @Test
    void resetPassword_withAnInvalidToken_changesNothing() {
        when(tokenService.redeem("bad", AccountTokenType.PASSWORD_RESET))
            .thenThrow(new BusinessRuleException("invalid"));

        assertThatThrownBy(() -> service.resetPassword("bad", "NewPassw0rd"))
            .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(userService, refreshTokenService, events);
    }

    private <T> T captured(Class<T> type) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(type);
        return type.cast(captor.getValue());
    }

    private static User user(long id, boolean verified) {
        User user = new User("aditya@example.com", "hash", "Aditya", "Kumar", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", id);
        if (verified) {
            user.markEmailVerified(NOW);
        }
        return user;
    }
}
