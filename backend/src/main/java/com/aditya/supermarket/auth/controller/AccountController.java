package com.aditya.supermarket.auth.controller;

import com.aditya.supermarket.auth.dto.EmailRequest;
import com.aditya.supermarket.auth.dto.MessageResponse;
import com.aditya.supermarket.auth.dto.ResetPasswordRequest;
import com.aditya.supermarket.auth.dto.VerifyEmailRequest;
import com.aditya.supermarket.auth.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public email-based account flows: verify email, resend the link, forgot / reset password. */
@Tag(name = "Account recovery")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AccountController {

    static final String RESEND_MESSAGE =
        "If this email belongs to an account that still needs verification, we have sent a new link.";
    static final String FORGOT_MESSAGE =
        "If an account exists for this email, we have sent a link to reset the password.";

    private final AccountService accountService;

    @Operation(summary = "Confirm an email address with the token from the verification email")
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        accountService.verifyEmail(request.token());
    }

    @Operation(summary = "Send the verification email again (same answer for every address)")
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest request) {
        accountService.resendVerification(request.email());
        return new MessageResponse(RESEND_MESSAGE);
    }

    @Operation(summary = "Email a password-reset link (same answer for every address)")
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        accountService.requestPasswordReset(request.email());
        return new MessageResponse(FORGOT_MESSAGE);
    }

    @Operation(summary = "Set a new password using the token from the reset email (signs out all devices)")
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request.token(), request.newPassword());
    }
}
