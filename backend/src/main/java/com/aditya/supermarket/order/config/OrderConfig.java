package com.aditya.supermarket.order.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the checkout configuration properties. */
@Configuration
@EnableConfigurationProperties(CheckoutProperties.class)
public class OrderConfig {
}
