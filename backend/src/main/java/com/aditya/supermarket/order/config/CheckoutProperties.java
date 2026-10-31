package com.aditya.supermarket.order.config;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Checkout settings ({@code app.checkout.*}).
 *
 * @param codShippingFee flat shipping charge added to a cash-on-delivery order (default 40, in rupees);
 *                       online-paid orders ship free
 */
@Validated
@ConfigurationProperties(prefix = "app.checkout")
public record CheckoutProperties(@DefaultValue("40") @DecimalMin("0.00") BigDecimal codShippingFee) {
}
