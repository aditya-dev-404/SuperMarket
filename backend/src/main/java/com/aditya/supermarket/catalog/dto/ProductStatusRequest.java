package com.aditya.supermarket.catalog.dto;

import com.aditya.supermarket.catalog.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(@NotNull ProductStatus status) {
}
