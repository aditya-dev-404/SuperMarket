package com.aditya.supermarket.notification.service;

/** The HTML and plain-text versions of one email. */
public record RenderedEmail(String html, String text) {
}
