package com.aditya.supermarket.cart.service;

import com.aditya.supermarket.cart.dto.CartItemStatus;
import com.aditya.supermarket.cart.entity.CartItem;
import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.catalog.entity.ProductStatus;
import com.aditya.supermarket.catalog.entity.ProductVariant;
import com.aditya.supermarket.vendor.entity.VendorStatus;
import java.math.BigDecimal;

/** Pure rules about whether a variant can be bought and what it costs. */
public final class CartItemRules {

    private CartItemRules() {
    }

    /** A variant is buyable only while it, its product and its vendor are all live. */
    public static boolean isPurchasable(ProductVariant variant) {
        Product product = variant.getProduct();
        return variant.isActive()
            && product.getStatus() == ProductStatus.ACTIVE
            && product.getVendor().getStatus() == VendorStatus.APPROVED;
    }

    /** Variant price override, or the product's base price when none is set. */
    public static BigDecimal unitPrice(ProductVariant variant) {
        return variant.getPrice() != null ? variant.getPrice() : variant.getProduct().getBasePrice();
    }

    public static CartItemStatus statusOf(CartItem item) {
        ProductVariant variant = item.getVariant();
        if (!isPurchasable(variant)) {
            return CartItemStatus.UNAVAILABLE;
        }
        if (variant.getStockQuantity() <= 0) {
            return CartItemStatus.OUT_OF_STOCK;
        }
        if (item.getQuantity() > variant.getStockQuantity()) {
            return CartItemStatus.INSUFFICIENT_STOCK;
        }
        return CartItemStatus.AVAILABLE;
    }
}
