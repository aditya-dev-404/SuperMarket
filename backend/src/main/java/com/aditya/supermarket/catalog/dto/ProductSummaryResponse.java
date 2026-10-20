package com.aditya.supermarket.catalog.dto;

import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.catalog.entity.ProductStatus;

import java.math.BigDecimal;

public record ProductSummaryResponse(Long id, String name, String slug, BigDecimal basePrice,
                                     ProductStatus status, String categoryName,
                                     String primaryImageUrl, int variantCount) {

    public static ProductSummaryResponse from(Product p) {
        String image = p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl();
        return new ProductSummaryResponse(p.getId(), p.getName(), p.getSlug(), p.getBasePrice(),
            p.getStatus(), p.getCategory().getName(), image, p.getVariants().size());
    }
}
