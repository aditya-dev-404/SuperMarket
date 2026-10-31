package com.aditya.supermarket.address.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the address configuration properties. */
@Configuration
@EnableConfigurationProperties(AddressProperties.class)
public class AddressConfig {
}
