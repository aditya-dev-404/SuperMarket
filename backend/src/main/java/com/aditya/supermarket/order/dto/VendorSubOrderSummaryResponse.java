package com.aditya.supermarket.order.dto;

import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.OrderItem;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.SubOrder;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row of the vendor's order list.
 *
 * @param itemCount     total units in this sub-order
 * @param recipientName who the parcel is addressed to
 */
public record VendorSubOrderSummaryResponse(
    Long id,
    String orderNumber,
    Instant createdAt,
    SubOrderStatus status,
    PaymentMethod paymentMethod,
    BigDecimal subtotal,
    int itemCount,
    String recipientName,
    String city) {

    public static VendorSubOrderSummaryResponse from(SubOrder subOrder) {
        Order order = subOrder.getCustomerOrder();
        return new VendorSubOrderSummaryResponse(
            subOrder.getId(),
            order.getOrderNumber(),
            subOrder.getCreatedAt(),
            subOrder.getStatus(),
            order.getPaymentMethod(),
            subOrder.getSubtotal(),
            subOrder.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
            order.getShippingAddress().getFullName(),
            order.getShippingAddress().getCity());
    }
}
