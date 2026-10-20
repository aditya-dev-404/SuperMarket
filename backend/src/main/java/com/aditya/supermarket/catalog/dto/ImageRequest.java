package com.aditya.supermarket.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageRequest(
    @NotBlank @Size(max = 500) String url,
    @NotBlank @Size(max = 255) String publicId) {
}
