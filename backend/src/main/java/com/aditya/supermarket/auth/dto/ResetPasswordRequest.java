package com.aditya.supermarket.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank @Size(max = 200) String token,
    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
        message = "Password must be 8-72 characters and contain a letter and a digit")
    String newPassword) {
}
