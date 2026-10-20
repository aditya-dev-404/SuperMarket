package com.aditya.supermarket.catalog.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

public final class SlugUtil {

    private SlugUtil() {
    }

    public static String slugify(String input) {
        String s = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "item" : s;
    }

    /** Slug with a short random suffix so two vendors can sell the same product name. */
    public static String uniqueSlug(String input) {
        return slugify(input) + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
