package com.aditya.supermarket.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data for the filter sidebar. Sizes / colours / price range are scoped to the
 * selected category (including its sub-categories) when one is given.
 */
public record StorefrontFiltersResponse(
    List<CategoryNode> categories,
    List<String> sizes,
    List<String> colours,
    BigDecimal minPrice,
    BigDecimal maxPrice) {

    /** Flat list; the client builds the tree using {@code parentSlug}. */
    public record CategoryNode(String name, String slug, String parentSlug) {
    }
}
