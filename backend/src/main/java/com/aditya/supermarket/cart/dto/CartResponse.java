package com.aditya.supermarket.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param totalQuantity all units in the cart (drives the header badge)
 * @param subtotal      sum of AVAILABLE lines only
 * @param checkoutReady true when the cart is not empty and every line is AVAILABLE
 */
public record CartResponse(
    List<CartVendorGroup> vendors,
    int totalQuantity,
    BigDecimal subtotal,
    boolean checkoutReady) {

    public static CartResponse empty() {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO, false);
    }
}
