package com.aditya.supermarket.order.dto;

import com.aditya.supermarket.order.entity.OrderItem;
import java.math.BigDecimal;

/**
 * @param productSlug lets the UI link back to the product page; {@code productId} is null once the vendor deleted the product
 */
public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    String productSlug,
    String sku,
    String size,
    String colour,
    String imageUrl,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductName(),
            item.getProductSlug(),
            item.getSku(),
            item.getSize(),
            item.getColour(),
            item.getImageUrl(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getLineTotal());
    }
}
