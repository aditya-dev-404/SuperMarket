package com.aditya.supermarket.notification.listener;

import static com.aditya.supermarket.notification.NotificationTestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.aditya.supermarket.notification.dto.NotificationRequest;
import com.aditya.supermarket.notification.entity.NotificationType;
import com.aditya.supermarket.notification.event.EmailVerificationRequestedEvent;
import com.aditya.supermarket.notification.event.EmailVerifiedEvent;
import com.aditya.supermarket.notification.event.PasswordChangedEvent;
import com.aditya.supermarket.notification.event.PasswordResetRequestedEvent;
import com.aditya.supermarket.notification.service.NotificationService;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountEmailListenerTest {

    @Mock
    private NotificationService notificationService;

    private AccountEmailListener listener;

    @BeforeEach
    void setUp() {
        listener = new AccountEmailListener(notificationService, properties());
    }

    @Test
    void verificationEmail_containsTheVerifyLinkAndValidity() {
        listener.onEmailVerificationRequested(new EmailVerificationRequestedEvent(
            42L, "aditya@example.com", "Aditya", "tok123", Duration.ofHours(24)));

        NotificationRequest request = sent();
        assertThat(request.type()).isEqualTo(NotificationType.EMAIL_VERIFICATION);
        assertThat(request.to()).isEqualTo("aditya@example.com");
        assertThat(request.template()).isEqualTo("generic");
        assertThat(request.referenceId()).isEqualTo("42");
        Map<String, Object> model = request.model();
        assertThat(model.get("actionUrl")).isEqualTo("http://localhost:5173/verify-email?token=tok123");
        assertThat(model.get("greeting")).isEqualTo("Hi Aditya,");
        assertThat(model.get("paragraphs").toString()).contains("24 hours");
    }

    @Test
    void passwordResetEmail_containsTheResetLinkAndValidity() {
        listener.onPasswordResetRequested(new PasswordResetRequestedEvent(
            42L, "aditya@example.com", "Aditya", "tok456", Duration.ofMinutes(30)));

        NotificationRequest request = sent();
        assertThat(request.type()).isEqualTo(NotificationType.PASSWORD_RESET);
        assertThat(request.model().get("actionUrl")).isEqualTo("http://localhost:5173/reset-password?token=tok456");
        assertThat(request.model().get("paragraphs").toString()).contains("30 minutes");
    }

    @Test
    void passwordChangedEmail_linksToForgotPassword() {
        listener.onPasswordChanged(new PasswordChangedEvent(42L, "aditya@example.com", "Aditya"));

        NotificationRequest request = sent();
        assertThat(request.type()).isEqualTo(NotificationType.PASSWORD_CHANGED);
        assertThat(request.model().get("actionUrl")).isEqualTo("http://localhost:5173/forgot-password");
    }

    @Test
    void welcomeEmail_hasNoFootnoteAndFallsBackToGenericGreeting() {
        listener.onEmailVerified(new EmailVerifiedEvent(42L, "aditya@example.com", " "));

        NotificationRequest request = sent();
        assertThat(request.type()).isEqualTo(NotificationType.WELCOME);
        assertThat(request.model()).doesNotContainKey("footnote");
        assertThat(request.model().get("greeting")).isEqualTo("Hi there,");
    }

    @Test
    void events_doNotRevealTheTokenInToString() {
        EmailVerificationRequestedEvent verification = new EmailVerificationRequestedEvent(
            1L, "a@example.com", "A", "secret-token", Duration.ofHours(1));
        PasswordResetRequestedEvent reset = new PasswordResetRequestedEvent(
            1L, "a@example.com", "A", "secret-token", Duration.ofHours(1));

        assertThat(verification.toString()).doesNotContain("secret-token");
        assertThat(reset.toString()).doesNotContain("secret-token");
    }

    @Test
    void describe_formatsDurationsForHumans() {
        assertThat(AccountEmailListener.describe(Duration.ofHours(24))).isEqualTo("24 hours");
        assertThat(AccountEmailListener.describe(Duration.ofHours(1))).isEqualTo("1 hour");
        assertThat(AccountEmailListener.describe(Duration.ofMinutes(30))).isEqualTo("30 minutes");
        assertThat(AccountEmailListener.describe(Duration.ofMinutes(1))).isEqualTo("1 minute");
    }

    private NotificationRequest sent() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).send(captor.capture());
        return captor.getValue();
    }
}
