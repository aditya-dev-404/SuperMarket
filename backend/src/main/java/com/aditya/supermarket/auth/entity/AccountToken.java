package com.aditya.supermarket.auth.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * A single-use token emailed to a user. Only the SHA-256 hash of the token is stored.
 * The user is referenced by id (not as an entity) so loading a token never loads a user.
 */
@Getter
@Entity
@Table(name = "account_tokens")
public class AccountToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountTokenType type;

    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected AccountToken() {
    }

    public static AccountToken issue(Long userId, AccountTokenType type, String tokenHash, Instant expiresAt) {
        AccountToken token = new AccountToken();
        token.userId = userId;
        token.type = type;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        return token;
    }
}
