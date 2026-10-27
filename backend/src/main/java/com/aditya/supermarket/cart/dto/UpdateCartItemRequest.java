package com.aditya.supermarket.cart.dto;

import jakarta.validation.constraints.Min;

/** Sets the absolute quantity of an existing cart line. */
public record UpdateCartItemRequest(@Min(1) int quantity) {
}
