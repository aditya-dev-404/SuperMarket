package com.aditya.supermarket.cart.dto;

/**
 * Why a cart line can or cannot be bought right now.
 *
 * <ul>
 *   <li>AVAILABLE - fine, counted in the subtotal
 *   <li>INSUFFICIENT_STOCK - some stock left but less than the quantity in the cart
 *   <li>OUT_OF_STOCK - no stock left
 *   <li>UNAVAILABLE - variant deactivated, product not active, or vendor not approved any more
 * </ul>
 */
public enum CartItemStatus {
    AVAILABLE,
    INSUFFICIENT_STOCK,
    OUT_OF_STOCK,
    UNAVAILABLE
}
