package com.aditya.supermarket.notification.listener;

import static com.aditya.supermarket.notification.NotificationTestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.notification.dto.NotificationRequest;
import com.aditya.supermarket.notification.entity.NotificationType;
import com.aditya.supermarket.notification.event.VendorApplicationApprovedEvent;
import com.aditya.supermarket.notification.event.VendorApplicationRejectedEvent;
import com.aditya.supermarket.notification.event.VendorApplicationSubmittedEvent;
import com.aditya.supermarket.notification.service.NotificationService;
import com.aditya.supermarket.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorEmailListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    private VendorEmailListener listener;

    @BeforeEach
    void setUp() {
        listener = new VendorEmailListener(notificationService, userService, properties());
    }

    @Test
    void submitted_sendsReceiptToApplicantAndOneAlertPerAdmin() {
        when(userService.findAdminEmails()).thenReturn(List.of("admin1@example.com", "admin2@example.com"));

        listener.onApplicationSubmitted(submitted(false, "29ABCDE1234F1Z5"));

        List<NotificationRequest> sent = sentRequests(3);
        NotificationRequest receipt = sent.get(0);
        assertThat(receipt.type()).isEqualTo(NotificationType.VENDOR_APPLICATION_RECEIVED);
        assertThat(receipt.to()).isEqualTo("aditya@example.com");
        assertThat(receipt.referenceId()).isEqualTo("7");
        assertThat(receipt.model().get("paragraphs").toString()).contains("Fresh Mart");
        assertThat(receipt.model().get("actionUrl")).isEqualTo("http://localhost:5173/become-vendor");

        assertThat(sent.subList(1, 3)).extracting(NotificationRequest::to)
            .containsExactly("admin1@example.com", "admin2@example.com");
        assertThat(sent.subList(1, 3)).allSatisfy(alert -> {
            assertThat(alert.type()).isEqualTo(NotificationType.VENDOR_APPLICATION_ADMIN_ALERT);
            assertThat(alert.subject()).isEqualTo("New vendor application: Fresh Mart");
            assertThat(alert.model().get("actionUrl"))
                .isEqualTo("http://localhost:5173/admin/vendor-applications");
            assertThat(alert.model().get("paragraphs").toString())
                .contains("Aditya Kumar", "aditya@example.com", "9876543210", "29ABCDE1234F1Z5");
        });
    }

    @Test
    void submitted_resubmissionIsDescribedAsSuch() {
        when(userService.findAdminEmails()).thenReturn(List.of("admin@example.com"));

        listener.onApplicationSubmitted(submitted(true, null));

        List<NotificationRequest> sent = sentRequests(2);
        assertThat(sent.get(0).model().get("paragraphs").toString()).contains("updated application");
        assertThat(sent.get(1).subject()).isEqualTo("Vendor application resubmitted: Fresh Mart");
        assertThat(sent.get(1).model().get("paragraphs").toString()).doesNotContain("GST");
    }

    @Test
    void submitted_withNoAdminStillSendsTheReceipt() {
        when(userService.findAdminEmails()).thenReturn(List.of());

        listener.onApplicationSubmitted(submitted(false, null));

        assertThat(sentRequests(1).get(0).type()).isEqualTo(NotificationType.VENDOR_APPLICATION_RECEIVED);
    }

    @Test
    void approved_linksToTheVendorDashboard() {
        listener.onApplicationApproved(new VendorApplicationApprovedEvent(
            7L, 42L, "aditya@example.com", "Aditya", "Fresh Mart"));

        NotificationRequest request = sentRequests(1).get(0);
        assertThat(request.type()).isEqualTo(NotificationType.VENDOR_APPROVED);
        assertThat(request.to()).isEqualTo("aditya@example.com");
        assertThat(request.model().get("actionUrl")).isEqualTo("http://localhost:5173/vendor/dashboard");
        assertThat(request.model().get("greeting")).isEqualTo("Hi Aditya,");
        assertThat(request.model().get("paragraphs").toString()).contains("Fresh Mart");
    }

    @Test
    void rejected_includesTheReasonAndLinksBackToTheForm() {
        listener.onApplicationRejected(new VendorApplicationRejectedEvent(
            7L, 42L, "aditya@example.com", "Aditya", "Fresh Mart", "GST number could not be verified"));

        NotificationRequest request = sentRequests(1).get(0);
        assertThat(request.type()).isEqualTo(NotificationType.VENDOR_REJECTED);
        assertThat(request.model().get("paragraphs").toString()).contains("GST number could not be verified");
        assertThat(request.model().get("actionUrl")).isEqualTo("http://localhost:5173/become-vendor");
    }

    @Test
    void approved_doesNotLookUpAdmins() {
        listener.onApplicationApproved(new VendorApplicationApprovedEvent(7L, 42L, "a@b.c", "A", "S"));

        verify(userService, never()).findAdminEmails();
    }

    private static VendorApplicationSubmittedEvent submitted(boolean resubmission, String gst) {
        return new VendorApplicationSubmittedEvent(7L, 42L, "aditya@example.com", "Aditya", "Aditya Kumar",
            "Fresh Mart", "9876543210", gst, resubmission);
    }

    private List<NotificationRequest> sentRequests(int expectedCount) {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(expectedCount)).send(captor.capture());
        return captor.getAllValues();
    }
}
