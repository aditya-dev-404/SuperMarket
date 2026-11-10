package com.aditya.supermarket.notification.config;

import static com.aditya.supermarket.notification.NotificationTestData.properties;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationPropertiesTest {

    @Test
    void link_joinsBaseUrlAndPathWithExactlyOneSlash() {
        assertThat(properties().link("/verify-email?token=abc"))
            .isEqualTo("http://localhost:5173/verify-email?token=abc");
        assertThat(properties().link("orders/5")).isEqualTo("http://localhost:5173/orders/5");
    }

    @Test
    void link_ignoresTrailingSlashOnBaseUrl() {
        NotificationProperties withSlash = new NotificationProperties(true, "shop@example.com", "superMarket",
            "https://shop.example.com/", 3, properties().retryDelay(), properties().retryInterval(), 20);

        assertThat(withSlash.link("/orders")).isEqualTo("https://shop.example.com/orders");
    }
}
