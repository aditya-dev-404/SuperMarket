package com.aditya.supermarket.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    void charge_succeeds_whenSuccessIsRequested() {
        PaymentResult result = gateway.charge("SM-TEST", new BigDecimal("200.00"), PaymentOutcome.SUCCESS);

        assertThat(result.successful()).isTrue();
        assertThat(result.reference()).startsWith("MOCK-").hasSize("MOCK-".length() + 12);
    }

    @Test
    void charge_fails_whenFailureIsRequested() {
        PaymentResult result = gateway.charge("SM-TEST", new BigDecimal("200.00"), PaymentOutcome.FAILURE);

        assertThat(result.successful()).isFalse();
    }

    @Test
    void charge_givesEveryAttemptItsOwnReference() {
        PaymentResult first = gateway.charge("SM-TEST", BigDecimal.TEN, PaymentOutcome.SUCCESS);
        PaymentResult second = gateway.charge("SM-TEST", BigDecimal.TEN, PaymentOutcome.SUCCESS);

        assertThat(first.reference()).isNotEqualTo(second.reference());
        assertThat(gateway.providerName()).isEqualTo("MOCK");
    }
}
