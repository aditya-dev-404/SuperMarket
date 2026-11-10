package com.aditya.supermarket.notification.service;

import static com.aditya.supermarket.notification.NotificationTestData.pendingLog;
import static com.aditya.supermarket.notification.NotificationTestData.properties;
import static com.aditya.supermarket.notification.NotificationTestData.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.notification.entity.NotificationLog;
import com.aditya.supermarket.notification.exception.EmailDeliveryException;
import com.aditya.supermarket.notification.sender.EmailMessage;
import com.aditya.supermarket.notification.sender.EmailSender;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final RenderedEmail RENDERED = new RenderedEmail("<p>html</p>", "text");

    @Mock
    private EmailTemplateRenderer renderer;
    @Mock
    private NotificationLogService logService;
    @Mock
    private EmailSender emailSender;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(properties(), renderer, logService, emailSender);
    }

    @Test
    void send_rendersLogsAndDelivers() {
        NotificationLog entry = pendingLog(7L);
        when(renderer.render(eq("generic"), any())).thenReturn(RENDERED);
        when(logService.createPending(any(), any(), any(), eq(RENDERED), any())).thenReturn(entry);

        service.send(request());

        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(message.capture());
        assertThat(message.getValue().to()).isEqualTo("aditya@example.com");
        assertThat(message.getValue().subject()).isEqualTo("Your password was changed");
        verify(logService).recordSent(7L);
    }

    @Test
    void send_recordsFailureAndDoesNotThrow_whenSmtpRejects() {
        NotificationLog entry = pendingLog(7L);
        when(renderer.render(eq("generic"), any())).thenReturn(RENDERED);
        when(logService.createPending(any(), any(), any(), any(), any())).thenReturn(entry);
        doThrow(new EmailDeliveryException("smtp down", null)).when(emailSender).send(any());

        service.send(request());

        verify(logService).recordFailure(eq(7L), any());
        verify(logService, never()).recordSent(any());
    }

    @Test
    void send_swallowsRenderingErrors_soBusinessFlowIsNeverBroken() {
        when(renderer.render(any(), any())).thenThrow(new IllegalStateException("template missing"));

        service.send(request());

        verifyNoInteractions(logService, emailSender);
    }

    @Test
    void send_doesNothing_whenNotificationsAreDisabled() {
        service = new NotificationService(properties(false, 3), renderer, logService, emailSender);

        service.send(request());

        verifyNoInteractions(renderer, logService, emailSender);
    }

    @Test
    void retryDue_deliversEveryDueEntry() {
        when(logService.findDue(anyInt())).thenReturn(List.of(pendingLog(1L), pendingLog(2L)));

        int handled = service.retryDue();

        assertThat(handled).isEqualTo(2);
        verify(logService).recordSent(1L);
        verify(logService).recordSent(2L);
    }

    @Test
    void retryDue_keepsGoing_whenOneEntryFails() {
        when(logService.findDue(anyInt())).thenReturn(List.of(pendingLog(1L), pendingLog(2L)));
        doThrow(new EmailDeliveryException("smtp down", null)).doNothing().when(emailSender).send(any());

        service.retryDue();

        verify(logService).recordFailure(eq(1L), any());
        verify(logService).recordSent(2L);
    }

    @Test
    void retryDue_doesNothing_whenNotificationsAreDisabled() {
        service = new NotificationService(properties(false, 3), renderer, logService, emailSender);

        assertThat(service.retryDue()).isZero();

        verifyNoInteractions(logService, emailSender);
    }
}
