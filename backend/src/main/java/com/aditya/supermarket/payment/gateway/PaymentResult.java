package com.aditya.supermarket.payment.gateway;

/**
 * @param reference the provider's id for this attempt (safe to store; never card data)
 */
public record PaymentResult(boolean successful, String reference) {
}
