package com.aditya.supermarket.notification.listener;

import com.aditya.supermarket.notification.config.NotificationConfig;
import com.aditya.supermarket.notification.config.NotificationProperties;
import com.aditya.supermarket.notification.dto.NotificationRequest;
import com.aditya.supermarket.notification.entity.NotificationType;
import com.aditya.supermarket.notification.event.VendorApplicationApprovedEvent;
import com.aditya.supermarket.notification.event.VendorApplicationRejectedEvent;
import com.aditya.supermarket.notification.event.VendorApplicationSubmittedEvent;
import com.aditya.supermarket.notification.service.NotificationService;
import com.aditya.supermarket.user.service.UserService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails for the vendor onboarding flow: a receipt to the applicant, an alert to every admin,
 * and the approval / rejection decision. Same rules as {@link AccountEmailListener}: runs after
 * commit and off the request thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VendorEmailListener {

    private final NotificationService notificationService;
    private final UserService userService;
    private final NotificationProperties properties;

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onApplicationSubmitted(VendorApplicationSubmittedEvent event) {
        sendReceiptToApplicant(event);
        alertAdmins(event);
    }

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onApplicationApproved(VendorApplicationApprovedEvent event) {
        send(NotificationType.VENDOR_APPROVED, event.applicantEmail(), event.vendorId(),
            "Your vendor application was approved",
            model("Your store is approved", event.applicantFirstName(),
                List.of("Good news: \"" + event.storeName() + "\" has been approved on "
                        + properties.fromName() + ".",
                    "Log in again to open your vendor dashboard, add products and start receiving orders."),
                "Open vendor dashboard", properties.link("/vendor/dashboard"), null));
    }

    @Async(NotificationConfig.EXECUTOR_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onApplicationRejected(VendorApplicationRejectedEvent event) {
        send(NotificationType.VENDOR_REJECTED, event.applicantEmail(), event.vendorId(),
            "Update on your vendor application",
            model("Your vendor application was not approved", event.applicantFirstName(),
                List.of("We could not approve \"" + event.storeName() + "\" at this time.",
                    "Reason given by our team: " + event.reason(),
                    "You can correct your details and resubmit the application at any time."),
                "Edit and resubmit", properties.link("/become-vendor"), null));
    }

    private void sendReceiptToApplicant(VendorApplicationSubmittedEvent event) {
        String intro = event.resubmission()
            ? "We received your updated application for \"" + event.storeName() + "\"."
            : "Thanks for applying to sell on " + properties.fromName() + ". We received your application for \""
            + event.storeName() + "\".";
        send(NotificationType.VENDOR_APPLICATION_RECEIVED, event.applicantEmail(), event.vendorId(),
            "We received your vendor application",
            model("Application received", event.applicantFirstName(),
                List.of(intro, "Our team will review it and email you as soon as there is a decision."),
                "View application status", properties.link("/become-vendor"), null));
    }

    /** One separate email per admin so recipients never see each other's addresses. */
    private void alertAdmins(VendorApplicationSubmittedEvent event) {
        List<String> admins = userService.findAdminEmails();
        if (admins.isEmpty()) {
            log.warn("Vendor application {} needs review but there is no active admin to notify", event.vendorId());
            return;
        }
        List<String> details = new ArrayList<>();
        details.add((event.resubmission() ? "A rejected application was resubmitted by " : "New application from ")
            + event.applicantFullName() + " (" + event.applicantEmail() + ").");
        details.add("Store: " + event.storeName() + ". Phone: " + event.contactPhone()
            + (event.gstNumber() == null ? "." : ". GST: " + event.gstNumber() + "."));
        String subject = (event.resubmission() ? "Vendor application resubmitted: " : "New vendor application: ")
            + event.storeName();
        for (String admin : admins) {
            send(NotificationType.VENDOR_APPLICATION_ADMIN_ALERT, admin, event.vendorId(), subject,
                model("Vendor application to review", null, details,
                    "Review application", properties.link("/admin/vendor-applications"), null));
        }
    }

    private void send(NotificationType type, String to, Long vendorId, String subject, Map<String, Object> model) {
        notificationService.send(new NotificationRequest(type, to, subject, "generic", model, String.valueOf(vendorId)));
    }

    private static Map<String, Object> model(String heading, String firstName, List<String> paragraphs,
                                             String actionLabel, String actionUrl, String footnote) {
        Map<String, Object> model = new HashMap<>();
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
}
