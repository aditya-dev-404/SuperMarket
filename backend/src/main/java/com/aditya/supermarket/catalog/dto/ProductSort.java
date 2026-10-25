package com.aditya.supermarket.catalog.dto;

import java.util.Locale;

/**
 * Whitelisted sort options for the public product list.
 * Only these values are ever turned into SQL, never raw user input.
 */
public enum ProductSort {
    NEWEST,
    PRICE_ASC,
    PRICE_DESC,
    RELEVANCE;

    /** Lenient parse: unknown or blank values return {@code null} so the caller can pick a default. */
    public static ProductSort fromParam(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
