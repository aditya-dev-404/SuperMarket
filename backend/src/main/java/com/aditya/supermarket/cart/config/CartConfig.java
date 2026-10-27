package com.aditya.supermarket.cart.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the cart configuration properties. */
@Configuration
@EnableConfigurationProperties(CartProperties.class)
public class CartConfig {
}
