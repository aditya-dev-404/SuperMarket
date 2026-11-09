package com.aditya.supermarket.auth.service;

import com.aditya.supermarket.auth.config.AccountProperties;
import com.aditya.supermarket.auth.entity.AccountToken;
import com.aditya.supermarket.auth.entity.AccountTokenType;
import com.aditya.supermarket.auth.repository.AccountTokenRepository;
import com.aditya.supermarket.common.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and redeems the one-time tokens behind email-verification and password-reset links.
 * The emailed token is random and unguessable; the database only ever sees its SHA-256 hash.
 */
@Service
@RequiredArgsConstructor
public class AccountTokenService {

    static final String INVALID_TOKEN_MESSAGE = "This link is invalid or has expired. Please request a new one.";
    private static final int TOKEN_BYTES = 32;

    private final AccountTokenRepository repository;
    private final AccountProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Always issues a new token and burns the user's older unused tokens of the same type. */
    @Transactional
    public IssuedAccountToken issue(Long userId, AccountTokenType type) {
        Instant now = clock.instant();
        Duration validFor = validityOf(type);
        repository.invalidateUnused(userId, type, now);

        String rawToken = newRawToken();
        Instant expiresAt = now.plus(validFor);
        repository.save(AccountToken.issue(userId, type, hash(rawToken), expiresAt));
        return new IssuedAccountToken(rawToken, expiresAt, validFor);
    }

    /**
     * Like {@link #issue} but returns empty when a token of this type was already issued within the
     * cooldown. Used by "resend" and "forgot password" so nobody can spam a mailbox. Callers answer
     * the same way either way, so the cooldown cannot be used to probe which emails exist.
     */
    @Transactional
    public Optional<IssuedAccountToken> issueUnlessRecentlyIssued(Long userId, AccountTokenType type) {
        Instant now = clock.instant();
        boolean recentlyIssued = repository.findFirstByUserIdAndTypeOrderByCreatedAtDesc(userId, type)
            .map(latest -> latest.getCreatedAt().plus(properties.emailCooldown()).isAfter(now))
            .orElse(false);
        return recentlyIssued ? Optional.empty() : Optional.of(issue(userId, type));
    }

    /**
     * Redeems a token exactly once and returns the id of the user it belongs to.
     *
     * @throws BusinessRuleException when the token is unknown, of another type, expired or already used
     *                               (deliberately one message for all cases)
     */
    @Transactional
    public Long redeem(String rawToken, AccountTokenType type) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessRuleException(INVALID_TOKEN_MESSAGE);
        }
        String tokenHash = hash(rawToken.strip());
        if (repository.markUsed(tokenHash, type, clock.instant()) == 0) {
            throw new BusinessRuleException(INVALID_TOKEN_MESSAGE);
        }
        return repository.findByTokenHash(tokenHash)
            .map(AccountToken::getUserId)
            .orElseThrow(() -> new BusinessRuleException(INVALID_TOKEN_MESSAGE));
    }

    /** SHA-256 of the token as 64 lowercase hex characters. */
    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Every Java runtime must provide SHA-256, so this cannot happen.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private Duration validityOf(AccountTokenType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> properties.verificationTokenTtl();
            case PASSWORD_RESET -> properties.passwordResetTokenTtl();
        };
    }

    private String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
