package com.aditya.supermarket.notification.event;

import com.aditya.supermarket.order.entity.SubOrderStatus;

/** Published when a vendor marks their part of an order as SHIPPED or DELIVERED. */
public record SubOrderStatusChangedEvent(Long orderId, Long subOrderId, SubOrderStatus status) {
}
