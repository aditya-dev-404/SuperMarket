package com.aditya.supermarket.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "Phone must be 7-20 digits, spaces, + or -")
        String phone) {
}
