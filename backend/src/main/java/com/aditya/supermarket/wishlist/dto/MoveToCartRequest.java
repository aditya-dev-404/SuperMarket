package com.aditya.supermarket.wishlist.dto;

import jakarta.validation.constraints.NotNull;

/** The size/colour variant chosen when moving a wishlist product to the cart (quantity 1). */
public record MoveToCartRequest(@NotNull Long variantId) {
}
