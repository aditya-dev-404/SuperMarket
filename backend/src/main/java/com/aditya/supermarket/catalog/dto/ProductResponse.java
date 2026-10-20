package com.aditya.supermarket.catalog.dto;

import com.aditya.supermarket.catalog.entity.Product;
import com.aditya.supermarket.catalog.entity.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(Long id, String name, String slug, String description, String brand,
                              BigDecimal basePrice, ProductStatus status,
                              Long categoryId, String categoryName,
                              List<VariantResponse> variants, List<ImageResponse> images) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getSlug(), p.getDescription(), p.getBrand(),
            p.getBasePrice(), p.getStatus(), p.getCategory().getId(), p.getCategory().getName(),
            p.getVariants().stream().map(VariantResponse::from).toList(),
            p.getImages().stream().map(ImageResponse::from).toList());
    }
}
