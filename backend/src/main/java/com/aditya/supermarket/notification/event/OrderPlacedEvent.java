package com.aditya.supermarket.notification.event;

/**
 * Published when an order becomes final for the vendors: right at checkout for cash on delivery,
 * after a successful payment for online orders. Triggers the customer's confirmation and one
 * "new order" email per vendor sub-order. Carries only the id; the listener loads the details.
 */
public record OrderPlacedEvent(Long orderId) {
}
