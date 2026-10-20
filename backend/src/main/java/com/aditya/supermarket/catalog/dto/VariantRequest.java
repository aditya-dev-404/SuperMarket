package com.aditya.supermarket.catalog.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record VariantRequest(
    @NotBlank @Size(max = 64) String sku,
    @Size(max = 30) String size,
    @Size(max = 30) String colour,
    @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
    @Min(0) int stockQuantity,
    boolean active) {
}
