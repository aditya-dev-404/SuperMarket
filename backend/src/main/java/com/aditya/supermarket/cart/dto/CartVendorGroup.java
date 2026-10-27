package com.aditya.supermarket.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/** Cart lines of one vendor; Phase 6 turns each group into a sub-order. */
public record CartVendorGroup(
    Long vendorId,
    String storeName,
    List<CartItemResponse> items,
    BigDecimal subtotal) {
}
