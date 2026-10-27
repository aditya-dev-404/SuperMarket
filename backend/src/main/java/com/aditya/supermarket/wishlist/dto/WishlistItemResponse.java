package com.aditya.supermarket.wishlist.dto;

import com.aditya.supermarket.catalog.dto.ProductCardResponse;
import java.time.Instant;

/** A wishlisted product, shaped like a storefront card so the UI can reuse the same component. */
public record WishlistItemResponse(ProductCardResponse product, Instant addedAt) {
}
