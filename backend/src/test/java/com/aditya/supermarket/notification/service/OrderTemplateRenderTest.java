package com.aditya.supermarket.notification.service;

import static com.aditya.supermarket.notification.NotificationTestData.properties;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Renders the real "order" templates, so a typo in a template fails here and not in production. */
class OrderTemplateRenderTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer(properties());

    @Test
    void render_fullOrderEmail_containsItemsTotalsAddressAndLink() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("heading", "Thank you for your order");
        model.put("greeting", "Hi Asha,");
        model.put("paragraphs", List.of("Your order SM-1 is confirmed."));
        model.put("items", List.of(
            item("T-Shirt", "M, Black", "2", "₹199.00", "₹398.00"),
            item("Jeans", null, "1", "₹299.00", "₹299.00")));
        model.put("summary", List.of(row("Subtotal", "₹697.00"), row("Shipping", "Free"), row("Total", "₹697.00")));
        model.put("addressTitle", "Delivering to");
        model.put("addressLines", List.of("Asha Rao", "Noida, Uttar Pradesh 201301"));
        model.put("actionLabel", "View my order");
        model.put("actionUrl", "http://localhost:5173/orders/100");

        RenderedEmail email = renderer.render("order", model);

        assertThat(email.html()).contains("Thank you for your order", "Hi Asha,", "T-Shirt", "M, Black", "Jeans",
            "₹398.00", "Subtotal", "Free", "Delivering to", "Asha Rao", "View my order",
            "http://localhost:5173/orders/100");
        assertThat(email.text()).contains("Thank you for your order", "Hi Asha,", "Items:",
                "2 x T-Shirt (M, Black) @ ₹199.00 = ₹398.00", "1 x Jeans @ ₹299.00 = ₹299.00",
                "Subtotal: ₹697.00", "Total: ₹697.00", "Delivering to:", "Asha Rao",
                "View my order: http://localhost:5173/orders/100")
            .doesNotContain("<");
    }

    @Test
    void render_withoutOptionalParts_stillRenders() {
        RenderedEmail email = renderer.render("order", Map.of(
            "heading", "Payment failed",
            "paragraphs", List.of("We could not take the payment.")));

        assertThat(email.html()).contains("Payment failed", "We could not take the payment.");
        assertThat(email.html()).doesNotContain("Delivering to");
        assertThat(email.text()).contains("Payment failed", "We could not take the payment.")
            .doesNotContain("Items:");
    }

    @Test
    void render_escapesMarkupInProductNames() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("heading", "Order");
        model.put("paragraphs", List.of("Hello"));
        model.put("items", List.of(item("<b>Evil</b>", null, "1", "₹1.00", "₹1.00")));

        RenderedEmail email = renderer.render("order", model);

        assertThat(email.html()).contains("&lt;b&gt;Evil&lt;/b&gt;").doesNotContain("<b>Evil</b>");
    }

    private static Map<String, String> item(String name, String variant, String quantity, String unit, String total) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        if (variant != null) {
            item.put("variant", variant);
        }
        item.put("quantity", quantity);
        item.put("unitPrice", unit);
        item.put("lineTotal", total);
        return item;
    }

    private static Map<String, String> row(String label, String value) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("value", value);
        return row;
    }
}
