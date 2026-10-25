package com.aditya.supermarket.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

/** Everything the public product page needs in one response. */
public record ProductDetailResponse(
    Long id,
    String name,
    String slug,
    String description,
    BigDecimal basePrice,
    VendorInfo vendor,
    List<CategoryCrumb> breadcrumb,
    List<VariantInfo> variants,
    List<ImageInfo> images) {

    public record VendorInfo(Long id, String name) {
    }

    public record CategoryCrumb(String name, String slug) {
    }

    /** {@code price} is already the effective price (variant override or product base price). */
    public record VariantInfo(
        Long id,
        String sku,
        String size,
        String colour,
        BigDecimal price,
        int stockQuantity,
        boolean inStock) {
    }

    public record ImageInfo(String url, int position) {
    }
}
