package com.aditya.supermarket.catalog.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 5000) String description,
    @Size(max = 100) String brand,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal basePrice,
    @NotNull Long categoryId) {
}
