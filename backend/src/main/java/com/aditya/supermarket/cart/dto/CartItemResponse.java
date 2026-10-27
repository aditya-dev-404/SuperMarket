package com.aditya.supermarket.cart.dto;

import java.math.BigDecimal;

/**
 * @param unitPrice     live price (variant override or product base price)
 * @param availableStock current stock of the variant, so the UI can cap the quantity stepper
 */
public record CartItemResponse(
    Long id,
    Long variantId,
    Long productId,
    String productName,
    String productSlug,
    String sku,
    String size,
    String colour,
    String imageUrl,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal,
    int availableStock,
    CartItemStatus status) {
}
