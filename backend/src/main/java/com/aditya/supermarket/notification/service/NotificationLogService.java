package com.aditya.supermarket.notification.service;

import com.aditya.supermarket.common.exception.ResourceNotFoundException;
import com.aditya.supermarket.notification.config.NotificationProperties;
import com.aditya.supermarket.notification.entity.NotificationLog;
import com.aditya.supermarket.notification.entity.NotificationStatus;
import com.aditya.supermarket.notification.entity.NotificationType;
import com.aditya.supermarket.notification.repository.NotificationLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database side of the notification log. Every method is its own short transaction so that
 * no database transaction is ever held open while talking to the SMTP server.
 */
@Service
@RequiredArgsConstructor
public class NotificationLogService {

    private final NotificationLogRepository repository;
    private final NotificationProperties properties;
    private final Clock clock;

    @Transactional
    public NotificationLog createPending(NotificationType type, String to, String subject,
                                         RenderedEmail email, String referenceId) {
        // The caller attempts delivery right away; this only protects against a crash before that.
        Instant firstRetryAt = clock.instant().plus(properties.retryDelay());
        return repository.save(NotificationLog.pending(
            type, to, subject, email.html(), email.text(), referenceId, firstRetryAt));
    }

    @Transactional
    public void recordSent(Long id) {
        find(id).markSent(clock.instant());
    }

    @Transactional
    public void recordFailure(Long id, String error) {
        find(id).markAttemptFailed(error, clock.instant(), properties.maxAttempts(), properties.retryDelay());
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> findDue(int limit) {
        return repository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            NotificationStatus.PENDING, clock.instant(), PageRequest.of(0, limit));
    }

    private NotificationLog find(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification log entry not found: " + id));
    }
}
