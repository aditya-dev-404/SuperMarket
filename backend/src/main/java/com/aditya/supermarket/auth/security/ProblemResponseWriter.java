package com.aditya.supermarket.auth.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;

/**
 * Writes RFC 9457 problem JSON from servlet filters, where {@code @RestControllerAdvice}
 * does not apply. Inputs are fixed strings, so no JSON escaping is needed.
 */
final class ProblemResponseWriter {

    private ProblemResponseWriter() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String title, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        String body = "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"timestamp\":\"%s\"}"
                .formatted(title, status.value(), detail, Instant.now());
        response.getWriter().write(body);
    }
}
