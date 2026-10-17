package com.aditya.supermarket.system.dto;

import java.time.Instant;

/**
 * Response of the public ping endpoint.
 *
 * @param status      always {@code UP} when the application is serving requests
 * @param application application name
 * @param timestamp   server time (UTC)
 */
public record PingResponse(String status, String application, Instant timestamp) {
}
