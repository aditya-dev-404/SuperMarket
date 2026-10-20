package com.aditya.supermarket.catalog.dto;

import com.aditya.supermarket.catalog.entity.ProductVariant;

import java.math.BigDecimal;

public record VariantResponse(Long id, String sku, String size, String colour,
                              BigDecimal price, int stockQuantity, boolean active) {

    public static VariantResponse from(ProductVariant v) {
        return new VariantResponse(v.getId(), v.getSku(), v.getSize(), v.getColour(),
            v.getPrice(), v.getStockQuantity(), v.isActive());
    }
}
