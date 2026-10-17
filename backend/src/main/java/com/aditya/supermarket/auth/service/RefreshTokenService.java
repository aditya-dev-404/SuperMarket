package com.aditya.supermarket.auth.service;

import com.aditya.supermarket.auth.config.JwtProperties;
import com.aditya.supermarket.auth.entity.RefreshToken;
import com.aditya.supermarket.auth.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and validates opaque refresh tokens with rotation: every use revokes the presented
 * token and a new one is issued. Presenting an already-revoked token means it was stolen or
 * replayed, so every session of that user is revoked.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String INVALID_MESSAGE = "Invalid or expired refresh token";

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    /** The raw token value (sent to the client once) and when it expires. */
    public record IssuedToken(String value, Instant expiresAt) {
    }

    @Transactional
    public IssuedToken issue(Long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now(clock).plus(properties.refreshTokenTtl());
        repository.save(new RefreshToken(hash(rawToken), userId, expiresAt));
        return new IssuedToken(rawToken, expiresAt);
    }

    /**
     * Validates the token and revokes it (rotation). Returns the owning user id.
     * {@code noRollbackFor} keeps the "revoke everything" side effect on reuse detection.
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Long consume(String rawToken) {
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadCredentialsException(INVALID_MESSAGE));
        if (token.isRevoked()) {
            repository.revokeAllForUser(token.getUserId());
            throw new BadCredentialsException(INVALID_MESSAGE);
        }
        if (token.isExpiredAt(Instant.now(clock))) {
            throw new BadCredentialsException(INVALID_MESSAGE);
        }
        token.revoke();
        return token.getUserId();
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
