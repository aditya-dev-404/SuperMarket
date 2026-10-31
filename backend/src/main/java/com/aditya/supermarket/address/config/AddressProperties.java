package com.aditya.supermarket.address.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Address settings ({@code app.address.*}).
 *
 * @param maxPerUser how many addresses one user may keep (default 10)
 */
@Validated
@ConfigurationProperties(prefix = "app.address")
public record AddressProperties(@DefaultValue("10") @Min(1) int maxPerUser) {
}
