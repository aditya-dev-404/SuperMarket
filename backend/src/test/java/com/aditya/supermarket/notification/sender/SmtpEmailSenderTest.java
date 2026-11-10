package com.aditya.supermarket.notification.sender;

import static com.aditya.supermarket.notification.NotificationTestData.properties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.notification.exception.EmailDeliveryException;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    private static final EmailMessage MESSAGE = new EmailMessage(
        "aditya@example.com", "Verify your email", "<p>Hello</p>", "Hello");

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailSender sender;

    @BeforeEach
    void setUp() {
        sender = new SmtpEmailSender(mailSender, properties());
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void send_buildsMessageFromApplicationMailbox() throws Exception {
        sender.send(MESSAGE);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage mime = sent.getValue();
        mime.saveChanges();
        assertThat(mime.getSubject()).isEqualTo("Verify your email");
        assertThat(mime.getRecipients(Message.RecipientType.TO))
            .extracting(Address::toString).containsExactly("aditya@example.com");
        assertThat(((InternetAddress) mime.getFrom()[0]).getAddress()).isEqualTo("shop@example.com");
        assertThat(((InternetAddress) mime.getFrom()[0]).getPersonal()).isEqualTo("superMarket");
    }

    @Test
    void send_wrapsMailErrorsInEmailDeliveryException() {
        doThrow(new MailSendException("connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send(MESSAGE))
            .isInstanceOf(EmailDeliveryException.class)
            .hasMessageContaining("connection refused");
    }
}
