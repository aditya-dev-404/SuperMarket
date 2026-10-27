package com.aditya.supermarket.cart.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Cart limits, bound from {@code app.cart.*}.
 *
 * @param maxQuantityPerLine upper bound for one variant in the cart (stock is checked separately)
 */
@Validated
@ConfigurationProperties(prefix = "app.cart")
public record CartProperties(@DefaultValue("10") @Min(1) int maxQuantityPerLine) {
}
