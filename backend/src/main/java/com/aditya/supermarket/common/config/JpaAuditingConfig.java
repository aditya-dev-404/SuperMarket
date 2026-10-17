package com.aditya.supermarket.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables {@code @CreatedDate} / {@code @LastModifiedDate} population. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
