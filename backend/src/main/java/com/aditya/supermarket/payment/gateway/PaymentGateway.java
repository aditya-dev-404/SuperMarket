package com.aditya.supermarket.payment.gateway;

import java.math.BigDecimal;

/**
 * The seam between orders and a payment provider. Phase 6 only has the mock implementation; Phase 10
 * replaces it with a real provider (hosted checkout plus a webhook) without touching order logic.
 */
public interface PaymentGateway {

    /** Short provider name stored with every payment attempt, e.g. MOCK. */
    String providerName();

    /**
     * Charges the amount for an order.
     *
     * @param requestedOutcome only the mock gateway uses this (to let you test both paths);
     *                         a real provider decides the outcome itself and ignores it
     */
    PaymentResult charge(String orderNumber, BigDecimal amount, PaymentOutcome requestedOutcome);
}
