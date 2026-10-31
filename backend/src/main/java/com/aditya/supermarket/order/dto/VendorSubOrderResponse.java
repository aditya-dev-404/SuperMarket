package com.aditya.supermarket.order.dto;

import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrder;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A vendor's own part of an order, with what is needed to pack and ship it: the items and the
 * delivery address (no customer account details).
 *
 * @param nextStatus the only status the vendor can set now, or null when the sub-order is finished
 *                   (DELIVERED or CANCELLED); drives the "Mark as ..." button
 */
public record VendorSubOrderResponse(
    Long id,
    String orderNumber,
    Instant orderedAt,
    SubOrderStatus status,
    SubOrderStatus nextStatus,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    BigDecimal subtotal,
    Instant shippedAt,
    Instant deliveredAt,
    Instant cancelledAt,
    ShippingAddressResponse shippingAddress,
    List<OrderItemResponse> items) {

    public static VendorSubOrderResponse from(SubOrder subOrder) {
        Order order = subOrder.getCustomerOrder();
        return new VendorSubOrderResponse(
            subOrder.getId(),
            order.getOrderNumber(),
            order.getCreatedAt(),
            subOrder.getStatus(),
            subOrder.nextStatus().orElse(null),
            order.getPaymentMethod(),
            order.getPaymentStatus(),
            subOrder.getSubtotal(),
            subOrder.getShippedAt(),
            subOrder.getDeliveredAt(),
            subOrder.getCancelledAt(),
            ShippingAddressResponse.from(order.getShippingAddress()),
            subOrder.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
