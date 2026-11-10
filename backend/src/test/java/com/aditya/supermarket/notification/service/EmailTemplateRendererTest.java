package com.aditya.supermarket.notification.service;

import static com.aditya.supermarket.notification.NotificationTestData.properties;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer(properties());

    @Test
    void render_genericTemplate_containsAllPartsInBothVersions() {
        RenderedEmail email = renderer.render("generic", Map.of(
            "heading", "Verify your email",
            "greeting", "Hi Aditya,",
            "paragraphs", List.of("Please confirm your address.", "The link expires in 24 hours."),
            "actionLabel", "Verify email",
            "actionUrl", "http://localhost:5173/verify-email?token=abc&x=1",
            "footnote", "If this was not you, ignore this email."));

        assertThat(email.html())
            .contains("Verify your email", "Hi Aditya,", "Please confirm your address.",
                "The link expires in 24 hours.", "Verify email", "superMarket",
                "If this was not you, ignore this email.");
        assertThat(email.html()).contains("http://localhost:5173/verify-email?token=abc&amp;x=1");
        assertThat(email.text())
            .contains("Verify your email", "Hi Aditya,", "Please confirm your address.",
                "Verify email: http://localhost:5173/verify-email?token=abc&x=1",
                "If this was not you, ignore this email.")
            .doesNotContain("<");
    }

    @Test
    void render_genericTemplate_omitsOptionalPartsWhenMissing() {
        RenderedEmail email = renderer.render("generic", Map.of(
            "heading", "Password changed",
            "paragraphs", List.of("Your password was changed.")));

        assertThat(email.html()).contains("Password changed", "Your password was changed.")
            .doesNotContain("copy this link");
        assertThat(email.text()).contains("Password changed", "Your password was changed.")
            .doesNotContain("null");
    }

    @Test
    void render_escapesUserSuppliedTextInHtml() {
        RenderedEmail email = renderer.render("generic", Map.of(
            "heading", "Hello",
            "greeting", "Hi <script>alert(1)</script>,"));

        assertThat(email.html()).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}
