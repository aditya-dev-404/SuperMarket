-- Phase 7: persisted outbox/log for transactional emails.
-- Rendered bodies are kept only while an email is still waiting to be delivered
-- (they can contain one-time links); they are cleared once the email is SENT or FAILED.

CREATE TABLE notification_log
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    type            VARCHAR(50)           NOT NULL,
    to_address      VARCHAR(255)          NOT NULL,
    subject         VARCHAR(255)          NOT NULL,
    html_body       MEDIUMTEXT            NULL,
    text_body       MEDIUMTEXT            NULL,
    status          VARCHAR(20)           NOT NULL,
    attempts        INT                   NOT NULL DEFAULT 0,
    reference_id    VARCHAR(100)          NULL,
    last_error      VARCHAR(500)          NULL,
    next_attempt_at DATETIME(6)           NULL,
    sent_at         DATETIME(6)           NULL,
    created_at      DATETIME(6)           NOT NULL,
    updated_at      DATETIME(6)           NOT NULL,
    CONSTRAINT pk_notification_log PRIMARY KEY (id),
    CONSTRAINT ck_notification_log_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_notification_log_attempts CHECK (attempts >= 0)
);

-- Used by the retry job: "pending emails that are due".
CREATE INDEX idx_notification_log_due ON notification_log (status, next_attempt_at);

-- Used to look up what was sent for an order / user / application.
CREATE INDEX idx_notification_log_reference ON notification_log (type, reference_id);
