package com.aditya.supermarket.order.dto;

import com.aditya.supermarket.order.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/** Checkout request: where to deliver and how to pay. The items always come from the user's cart. */
public record PlaceOrderRequest(
    @NotNull Long addressId,
    @NotNull PaymentMethod paymentMethod) {
}
