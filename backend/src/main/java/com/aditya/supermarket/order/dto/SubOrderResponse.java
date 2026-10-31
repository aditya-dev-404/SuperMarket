package com.aditya.supermarket.order.dto;

import com.aditya.supermarket.order.entity.SubOrder;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** One vendor's part of an order, as the customer sees it. */
public record SubOrderResponse(
    Long id,
    Long vendorId,
    String storeName,
    SubOrderStatus status,
    BigDecimal subtotal,
    Instant shippedAt,
    Instant deliveredAt,
    Instant cancelledAt,
    List<OrderItemResponse> items) {

    public static SubOrderResponse from(SubOrder subOrder) {
        return new SubOrderResponse(
            subOrder.getId(),
            subOrder.getVendor().getId(),
            subOrder.getVendor().getStoreName(),
            subOrder.getStatus(),
            subOrder.getSubtotal(),
            subOrder.getShippedAt(),
            subOrder.getDeliveredAt(),
            subOrder.getCancelledAt(),
            subOrder.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
