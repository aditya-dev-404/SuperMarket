package com.aditya.supermarket.notification.event;

/** Published when the payment of an online order failed and the order was cancelled. */
public record OrderPaymentFailedEvent(Long orderId) {
}
