package com.aditya.supermarket.notification.sender;

import com.aditya.supermarket.notification.exception.EmailDeliveryException;

/** Delivers one rendered email. Kept as an interface so the transport can be swapped or mocked. */
public interface EmailSender {

    /**
     * @throws EmailDeliveryException when the mail server did not accept the message
     */
    void send(EmailMessage message);
}
