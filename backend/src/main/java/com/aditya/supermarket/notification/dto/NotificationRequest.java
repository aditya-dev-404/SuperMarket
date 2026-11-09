package com.aditya.supermarket.notification.dto;

import com.aditya.supermarket.notification.entity.NotificationType;
import java.util.Map;

/**
 * What a feature asks the notification module to send.
 *
 * @param type        kind of email (stored in the log)
 * @param to          recipient address
 * @param subject     subject line
 * @param template    template name without folder or extension, e.g. {@code "generic"}
 * @param model       variables available to the template
 * @param referenceId optional id of the thing this email is about (order number, user id, ...)
 */
public record NotificationRequest(
    NotificationType type,
    String to,
    String subject,
    String template,
    Map<String, Object> model,
    String referenceId) {

    public NotificationRequest {
        model = model == null ? Map.of() : Map.copyOf(model);
    }
}
