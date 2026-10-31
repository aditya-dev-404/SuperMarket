package com.aditya.supermarket.order.dto;

import com.aditya.supermarket.order.entity.Order;
import com.aditya.supermarket.order.entity.OrderItem;
import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One row of "My orders".
 *
 * @param itemCount  total units across all items
 * @param thumbnails up to three product images to show next to the order
 */
public record OrderSummaryResponse(
    Long id,
    String orderNumber,
    Instant createdAt,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus,
    SubOrderStatus status,
    BigDecimal total,
    int itemCount,
    List<String> thumbnails) {

    private static final int MAX_THUMBNAILS = 3;

    public static OrderSummaryResponse from(Order order) {
        List<OrderItem> items = order.getSubOrders().stream()
            .flatMap(subOrder -> subOrder.getItems().stream())
            .toList();
        return new OrderSummaryResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCreatedAt(),
            order.getPaymentMethod(),
            order.getPaymentStatus(),
            order.overallStatus(),
            order.getTotal(),
            items.stream().mapToInt(OrderItem::getQuantity).sum(),
            items.stream()
                .map(OrderItem::getImageUrl)
                .filter(Objects::nonNull)
                .distinct()
                .limit(MAX_THUMBNAILS)
                .toList());
    }
}
