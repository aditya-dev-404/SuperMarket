package com.aditya.supermarket.catalog.dto;

import java.math.BigDecimal;


/**
 * Light-weight product card for list pages. Only what the card needs.
 *
 * @param price       lowest effective price across the product's active variants
 * @param priceVaries true when variants have different prices (UI shows "From ₹X")
 * @param inStock     false when every active variant has zero stock (UI greys the card out)
 */
public record ProductCardResponse(
    Long id,
    String name,
    String slug,
    BigDecimal price,
    boolean priceVaries,
    String imageUrl,
    boolean inStock) {
}
