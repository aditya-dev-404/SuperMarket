package com.aditya.supermarket.notification.service;

import com.aditya.supermarket.notification.config.NotificationProperties;
import com.aditya.supermarket.notification.dto.NotificationRequest;
import com.aditya.supermarket.notification.entity.NotificationLog;
import com.aditya.supermarket.notification.exception.EmailDeliveryException;
import com.aditya.supermarket.notification.sender.EmailMessage;
import com.aditya.supermarket.notification.sender.EmailSender;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Entry point of the notification module: renders an email, writes it to the notification log and
 * tries to deliver it. Delivery failures never reach the caller; a failed email is retried later by
 * {@link #retryDue()}. Intentionally not transactional (see {@link NotificationLogService}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationProperties properties;
    private final EmailTemplateRenderer renderer;
    private final NotificationLogService logService;
    private final EmailSender emailSender;

    /** Renders, logs and delivers one email. Never throws: a broken email must not break the business flow. */
    public void send(NotificationRequest request) {
        if (!properties.enabled()) {
            log.debug("Notifications disabled, skipping {} email", request.type());
            return;
        }
        try {
            RenderedEmail email = renderer.render(request.template(), request.model());
            NotificationLog entry = logService.createPending(
                request.type(), request.to(), request.subject(), email, request.referenceId());
            deliver(entry);
        } catch (RuntimeException e) {
            log.error("Could not queue {} email (reference {})", request.type(), request.referenceId(), e);
        }
    }

    /** Retries emails whose next attempt is due. Called by the scheduled retry job. */
    public int retryDue() {
        if (!properties.enabled()) {
            return 0;
        }
        List<NotificationLog> due = logService.findDue(properties.retryBatchSize());
        for (NotificationLog entry : due) {
            try {
                deliver(entry);
            } catch (RuntimeException e) {
                log.error("Retry of notification {} failed unexpectedly", entry.getId(), e);
            }
        }
        return due.size();
    }

    private void deliver(NotificationLog entry) {
        try {
            emailSender.send(new EmailMessage(
                entry.getToAddress(), entry.getSubject(), entry.getHtmlBody(), entry.getTextBody()));
            logService.recordSent(entry.getId());
        } catch (EmailDeliveryException e) {
            logService.recordFailure(entry.getId(), e.getMessage());
            log.warn("Delivery of {} email (log id {}) failed: {}", entry.getType(), entry.getId(), e.getMessage());
        }
    }
}
