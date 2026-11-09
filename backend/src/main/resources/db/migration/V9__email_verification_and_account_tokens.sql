-- Phase 7: email verification for new accounts + one-time tokens for verification / password reset.

ALTER TABLE users
    ADD COLUMN email_verified    BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified_at DATETIME(6) NULL;

-- Everyone who exists before this migration (including the seeded admin) keeps working:
-- they are treated as verified. Only accounts created afterwards start unverified.
UPDATE users
SET email_verified    = TRUE,
    email_verified_at = UTC_TIMESTAMP(6);

-- Only the SHA-256 hash of a token is stored, never the token that was emailed.
CREATE TABLE account_tokens
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    user_id    BIGINT                NOT NULL,
    type       VARCHAR(30)           NOT NULL,
    token_hash CHAR(64)              NOT NULL,
    expires_at DATETIME(6)           NOT NULL,
    used_at    DATETIME(6)           NULL,
    created_at DATETIME(6)           NOT NULL,
    updated_at DATETIME(6)           NOT NULL,
    CONSTRAINT pk_account_tokens PRIMARY KEY (id),
    CONSTRAINT uk_account_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_account_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_account_tokens_type CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

-- "latest token of this type for this user" (cooldown) and "invalidate unused tokens".
CREATE INDEX idx_account_tokens_user_type ON account_tokens (user_id, type);
