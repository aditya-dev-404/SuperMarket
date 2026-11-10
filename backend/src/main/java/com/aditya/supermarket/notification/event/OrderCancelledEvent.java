package com.aditya.supermarket.notification.event;

/** Published when the customer cancelled an order. */
public record OrderCancelledEvent(Long orderId) {
}
