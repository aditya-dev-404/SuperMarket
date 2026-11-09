package com.aditya.supermarket.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request carrying only an email address (resend verification, forgot password). */
public record EmailRequest(@NotBlank @Email @Size(max = 255) String email) {
}
