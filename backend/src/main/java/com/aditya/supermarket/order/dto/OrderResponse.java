package com.aditya.supermarket.order.dto;

import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Full order for the customer.
 *
 * @param status      overall progress: the least advanced active sub-order, or CANCELLED when all are cancelled
 * @param payable     true while an online order still waits for payment (show the "Pay now" button)
 * @param cancellable true while no vendor has shipped anything (show the "Cancel order" button)
 */
public record OrderResponse(
    Long id,
    String orderNumber,
    Instant createdAt,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    SubOrderStatus status,
    BigDecimal subtotal,
    BigDecimal shippingFee,
    BigDecimal total,
    boolean payable,
    boolean cancellable,
    ShippingAddressResponse shippingAddress,
    List<SubOrderResponse> subOrders) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCreatedAt(),
            order.getPaymentMethod(),
            order.getPaymentStatus(),
            order.overallStatus(),
            order.getSubtotal(),
            order.getShippingFee(),
            order.getTotal(),
            order.isPayable(),
            order.isCancellable(),
            ShippingAddressResponse.from(order.getShippingAddress()),
            order.getSubOrders().stream().map(SubOrderResponse::from).toList());
    }
}
