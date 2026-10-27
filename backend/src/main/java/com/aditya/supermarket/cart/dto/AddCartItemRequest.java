package com.aditya.supermarket.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Adds {@code quantity} more of a variant; merges into an existing line for the same variant. */
public record AddCartItemRequest(
    @NotNull Long variantId,
    @Min(1) int quantity) {
}
