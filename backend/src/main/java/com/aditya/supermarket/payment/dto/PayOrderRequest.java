package com.aditya.supermarket.payment.dto;

import com.aditya.supermarket.payment.gateway.PaymentOutcome;
import jakarta.validation.constraints.NotNull;

/** Body of the mock payment call: which result to simulate. */
public record PayOrderRequest(@NotNull PaymentOutcome outcome) {
}
