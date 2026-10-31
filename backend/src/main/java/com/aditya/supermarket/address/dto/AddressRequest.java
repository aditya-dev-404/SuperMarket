package com.aditya.supermarket.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update body for an address (India only for now).
 *
 * @param phone          10-digit Indian mobile number
 * @param pincode        6-digit Indian PIN code
 * @param defaultAddress true to make this the default address; false never removes an existing default
 */
public record AddressRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number") String phone,
    @NotBlank @Size(max = 200) String line1,
    @Size(max = 200) String line2,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String state,
    @NotBlank @Pattern(regexp = "^[1-9]\\d{5}$", message = "Enter a valid 6-digit PIN code") String pincode,
    boolean defaultAddress) {
}
