package com.aditya.supermarket.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VendorApplicationRequest(
    @NotBlank @Size(min = 3, max = 100) String storeName,
    @Size(max = 1000) String storeDescription,
    @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Enter a valid phone number") String contactPhone,
    @NotBlank @Size(max = 500) String businessAddress,
    @Pattern(regexp = "^$|^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z][1-9A-Za-z][Zz][0-9A-Za-z]$",
        message = "Enter a valid 15-character GSTIN") String gstNumber
) {
}
