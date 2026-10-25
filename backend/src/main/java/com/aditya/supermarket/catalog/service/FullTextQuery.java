package com.aditya.supermarket.catalog.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Turns free text typed by a shopper into a safe MySQL FULLTEXT boolean-mode string.
 *
 * <p>Every character that is not a letter, combining mark or digit is treated as a separator, so boolean
 * operators ({@code + - < > ( ) ~ * " @}) typed by the user can never change the query.
 * Each word becomes a required prefix term, e.g. {@code "red t-shirt"} → {@code "+red* +shirt*"}
 * (so typing "shir" still finds "shirt"). Single-character words are dropped because
 * InnoDB does not index them.
 */
final class FullTextQuery {

    private static final int MAX_TERMS = 8;
    private static final int MAX_TERM_LENGTH = 40;

    private FullTextQuery() {
    }

    static Optional<String> toBooleanMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        List<String> terms = Arrays.stream(raw.trim().split("[^\\p{L}\\p{M}\\p{N}]+"))
            .filter(word -> word.length() > 1)
            .limit(MAX_TERMS)
            .map(word -> word.length() > MAX_TERM_LENGTH ? word.substring(0, MAX_TERM_LENGTH) : word)
            .map(word -> "+" + word + "*")
            .toList();
        return terms.isEmpty() ? Optional.empty() : Optional.of(String.join(" ", terms));
    }
}
