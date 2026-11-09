package com.aditya.supermarket.notification.entity;

import com.aditya.supermarket.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;

/**
 * One email that was (or is waiting to be) sent. Doubles as an outbox: the rendered body is kept
 * only while the email is {@link NotificationStatus#PENDING}, because it may contain one-time links.
 */
@Getter
@Entity
@Table(name = "notification_log")
public class NotificationLog extends BaseEntity {

    /** Length that makes Hibernate map the column to MEDIUMTEXT on MySQL (matches V8). */
    private static final int BODY_LENGTH = 16_777_215;
    private static final int ERROR_LENGTH = 500;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "to_address", nullable = false)
    private String toAddress;

    @Column(nullable = false)
    private String subject;

    @Column(name = "html_body", length = BODY_LENGTH)
    private String htmlBody;

    @Column(name = "text_body", length = BODY_LENGTH)
    private String textBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "last_error", length = ERROR_LENGTH)
    private String lastError;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected NotificationLog() {
    }

    /**
     * Creates a PENDING entry. {@code firstRetryAt} is when the retry job may pick it up if the
     * immediate first attempt never reports back (e.g. the application stopped mid-way).
     */
    public static NotificationLog pending(NotificationType type, String toAddress, String subject,
                                          String htmlBody, String textBody, String referenceId,
                                          Instant firstRetryAt) {
        NotificationLog entry = new NotificationLog();
        entry.type = type;
        entry.toAddress = toAddress;
        entry.subject = subject;
        entry.htmlBody = htmlBody;
        entry.textBody = textBody;
        entry.referenceId = referenceId;
        entry.status = NotificationStatus.PENDING;
        entry.attempts = 0;
        entry.nextAttemptAt = firstRetryAt;
        return entry;
    }

    public void markSent(Instant now) {
        attempts++;
        status = NotificationStatus.SENT;
        sentAt = now;
        nextAttemptAt = null;
        lastError = null;
        clearBodies();
    }

    /**
     * Records a failed attempt. Below {@code maxAttempts} the email stays PENDING and is retried later
     * with a linear back-off ({@code baseDelay x attempts}); otherwise it is marked FAILED for good.
     */
    public void markAttemptFailed(String error, Instant now, int maxAttempts, Duration baseDelay) {
        attempts++;
        lastError = truncate(error);
        if (attempts >= maxAttempts) {
            status = NotificationStatus.FAILED;
            nextAttemptAt = null;
            clearBodies();
        } else {
            nextAttemptAt = now.plus(baseDelay.multipliedBy(attempts));
        }
    }

    private void clearBodies() {
        htmlBody = null;
        textBody = null;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= ERROR_LENGTH ? value : value.substring(0, ERROR_LENGTH);
    }
}
