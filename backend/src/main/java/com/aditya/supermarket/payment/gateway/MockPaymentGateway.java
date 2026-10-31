package com.aditya.supermarket.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pretends to be a payment provider: success or failure is whatever the caller asks for.
 * Active unless {@code app.payment.provider} names another provider, so a production setup that
 * forgets to configure a real gateway fails at startup instead of trusting the client's choice.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private static final String PROVIDER = "MOCK";
    private static final int REFERENCE_LENGTH = 12;

    @Override
    public String providerName() {
        return PROVIDER;
    }

    @Override
    public PaymentResult charge(String orderNumber, BigDecimal amount, PaymentOutcome requestedOutcome) {
        String reference = "MOCK-" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, REFERENCE_LENGTH).toUpperCase();
        return new PaymentResult(requestedOutcome == PaymentOutcome.SUCCESS, reference);
    }
}
