package com.aditya.supermarket.catalog.dto;

import java.math.BigDecimal;

/**
 * Normalised search input. Built by {@code StorefrontService}; all values are already
 * trimmed/clamped, and {@code fullTextQuery} is already a safe MySQL boolean-mode string.
 */
public record ProductSearchCriteria(
    String fullTextQuery,
    String categorySlug,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String variantSize,
    String variantColour,
    boolean inStockOnly,
    ProductSort sort,
    int page,
    int pageSize) {
}
