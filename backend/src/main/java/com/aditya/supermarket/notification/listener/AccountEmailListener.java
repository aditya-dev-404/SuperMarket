package com.aditya.supermarket.notification.listener;

import com.aditya.supermarket.notification.config.NotificationConfig;
import com.aditya.supermarket.notification.config.NotificationProperties;
import com.aditya.supermarket.notification.dto.NotificationRequest;
import com.aditya.supermarket.notification.entity.NotificationType;
import com.aditya.supermarket.notification.event.EmailVerificationRequestedEvent;
import com.aditya.supermarket.notification.event.EmailVerifiedEvent;
import com.aditya.supermarket.notification.event.PasswordChangedEvent;
import com.aditya.supermarket.notification.event.PasswordResetRequestedEvent;
import com.aditya.supermarket.notification.service.NotificationService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns account events into emails. Runs after the publishing transaction committed (so a rolled
 * back signup sends nothing) and on the notification thread pool (so the HTTP request never waits
 * for the mail server). {@code fallbackExecution} keeps events working if one is ever published
 * outside a transaction.
 */
@Component
@RequiredArgsConstructor
public class AccountEmailListener {

    private final NotificationService notificationService;
    private final NotificationProperties properties;

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        send(NotificationType.EMAIL_VERIFICATION, event.email(), event.userId(),
            "Verify your email address",
            model("Verify your email address", event.firstName(),
                List.of("Thanks for signing up for " + properties.fromName()
                        + ". Please confirm that this is your email address so you can log in.",
                    "This link is valid for " + describe(event.validFor()) + " and can be used once."),
                "Verify email", properties.link("/verify-email?token=" + event.token()),
                "If you did not create an account, you can safely ignore this email."));
    }

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        send(NotificationType.PASSWORD_RESET, event.email(), event.userId(),
            "Reset your password",
            model("Reset your password", event.firstName(),
                List.of("We received a request to reset the password for your account.",
                    "This link is valid for " + describe(event.validFor()) + " and can be used once."),
                "Reset password", properties.link("/reset-password?token=" + event.token()),
                "If you did not request this, you can ignore this email. Your password will not change."));
    }

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordChanged(PasswordChangedEvent event) {
        send(NotificationType.PASSWORD_CHANGED, event.email(), event.userId(),
            "Your password was changed",
            model("Your password was changed", event.firstName(),
                List.of("The password for your " + properties.fromName() + " account was just changed.",
                    "If this was you, no action is needed."),
                "Reset password", properties.link("/forgot-password"),
                "If this was not you, reset your password right away using the button above."));
    }

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmailVerified(EmailVerifiedEvent event) {
        send(NotificationType.WELCOME, event.email(), event.userId(),
            "Welcome to " + properties.fromName(),
            model("Welcome to " + properties.fromName(), event.firstName(),
                List.of("Your email is verified and your account is ready to use."),
                "Start shopping", properties.link("/"), null));
    }

    private void send(NotificationType type, String to, Long userId, String subject, Map<String, Object> model) {
        notificationService.send(new NotificationRequest(
            type, to, subject, "generic", model, String.valueOf(userId)));
    }

    private static Map<String, Object> model(String heading, String firstName, List<String> paragraphs,
                                             String actionLabel, String actionUrl, String footnote) {
        Map<String, Object> model = new java.util.HashMap<>();
        model.put("heading", heading);
        model.put("greeting", firstName == null || firstName.isBlank() ? "Hi there," : "Hi " + firstName + ",");
        model.put("paragraphs", paragraphs);
        model.put("actionLabel", actionLabel);
        model.put("actionUrl", actionUrl);
        if (footnote != null) {
            model.put("footnote", footnote);
        }
        return model;
    }

    /** "24 hours", "1 hour", "30 minutes". */
    static String describe(Duration duration) {
        long minutes = duration.toMinutes();
        if (minutes >= 60 && minutes % 60 == 0) {
            long hours = minutes / 60;
            return hours + (hours == 1 ? " hour" : " hours");
        }
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
}
