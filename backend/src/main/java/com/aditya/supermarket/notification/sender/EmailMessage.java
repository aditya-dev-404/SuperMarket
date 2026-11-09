package com.aditya.supermarket.notification.sender;

/** A fully rendered email, ready to hand to an {@link EmailSender}. */
public record EmailMessage(String to, String subject, String htmlBody, String textBody) {
}
