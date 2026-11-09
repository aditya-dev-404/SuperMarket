package com.aditya.supermarket.auth.service;

import com.aditya.supermarket.auth.dto.RegisterRequest;
import com.aditya.supermarket.auth.entity.AccountTokenType;
import com.aditya.supermarket.notification.event.EmailVerificationRequestedEvent;
import com.aditya.supermarket.notification.event.EmailVerifiedEvent;
import com.aditya.supermarket.notification.event.PasswordChangedEvent;
import com.aditya.supermarket.notification.event.PasswordResetRequestedEvent;
import com.aditya.supermarket.user.entity.User;
import com.aditya.supermarket.user.service.UserService;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email-based account flows: sign-up with email verification, resending the verification link and
 * password reset. Emails are sent by publishing events that are handled after the transaction commits.
 *
 * <p>"Resend" and "forgot password" never reveal whether an address has an account: they do nothing
 * for unknown addresses and the controller answers identically either way.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserService userService;
    private final AccountTokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Creates an unverified customer account and emails the verification link. The user is not logged in.
     * If the address already belongs to an <em>unverified</em> account, nothing is changed (a stranger must
     * not be able to overwrite someone else's pending password) and only the link is sent again.
     * A verified address is rejected with a 409 as before.
     */
    @Transactional
    public void register(RegisterRequest request) {
        Optional<User> existing = userService.findByEmail(request.email());
        if (existing.isPresent() && !existing.get().isEmailVerified()) {
            sendVerification(existing.get(), true);
            return;
        }
        User user = userService.registerCustomer(
            request.email(), request.password(), request.firstName(), request.lastName());
        sendVerification(user, false);
    }

    /** Confirms the address behind a verification link. Idempotent for already verified accounts. */
    @Transactional
    public void verifyEmail(String rawToken) {
        Long userId = tokenService.redeem(rawToken, AccountTokenType.EMAIL_VERIFICATION);
        User user = userService.getById(userId);
        if (user.isEmailVerified()) {
            return;
        }
        userService.markEmailVerified(userId, clock.instant());
        events.publishEvent(new EmailVerifiedEvent(user.getId(), user.getEmail(), user.getFirstName()));
    }

    @Transactional
    public void resendVerification(String email) {
        userService.findByEmail(email)
            .filter(user -> !user.isEmailVerified())
            .ifPresent(user -> sendVerification(user, true));
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userService.findByEmail(email)
            .filter(User::isEnabled)
            .flatMap(user -> tokenService.issueUnlessRecentlyIssued(user.getId(), AccountTokenType.PASSWORD_RESET)
                .map(token -> new PasswordResetRequestedEvent(
                    user.getId(), user.getEmail(), user.getFirstName(), token.rawToken(), token.validFor())))
            .ifPresent(events::publishEvent);
    }

    /** Sets a new password, signs the user out of every device and sends a security alert. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Long userId = tokenService.redeem(rawToken, AccountTokenType.PASSWORD_RESET);
        User user = userService.resetPassword(userId, newPassword, clock.instant());
        refreshTokenService.revokeAllForUser(userId);
        events.publishEvent(new PasswordChangedEvent(user.getId(), user.getEmail(), user.getFirstName()));
    }

    private void sendVerification(User user, boolean respectCooldown) {
        Optional<IssuedAccountToken> issued = respectCooldown
            ? tokenService.issueUnlessRecentlyIssued(user.getId(), AccountTokenType.EMAIL_VERIFICATION)
            : Optional.of(tokenService.issue(user.getId(), AccountTokenType.EMAIL_VERIFICATION));
        issued.ifPresent(token -> events.publishEvent(new EmailVerificationRequestedEvent(
            user.getId(), user.getEmail(), user.getFirstName(), token.rawToken(), token.validFor())));
    }
}
