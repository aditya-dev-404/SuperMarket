package com.aditya.supermarket.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata shown in Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI superMarketOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SuperMarket API")
                .description("Multi-vendor e-commerce marketplace REST API")
                .version("v1"));
    }
}
