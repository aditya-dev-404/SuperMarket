package com.aditya.supermarket.notification.sender;

import com.aditya.supermarket.notification.config.NotificationProperties;
import com.aditya.supermarket.notification.exception.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Sends multipart (plain text + HTML) emails over SMTP from the single application mailbox. */
@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.fromAddress(), properties.fromName());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), message.htmlBody());
            mailSender.send(mime);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new EmailDeliveryException("SMTP delivery failed: " + e.getMessage(), e);
        }
    }
}
