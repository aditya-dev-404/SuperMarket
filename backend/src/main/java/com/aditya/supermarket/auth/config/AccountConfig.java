package com.aditya.supermarket.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link AccountProperties}. */
@Configuration
@EnableConfigurationProperties(AccountProperties.class)
public class AccountConfig {
}
