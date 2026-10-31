package com.aditya.supermarket.order.service;

import com.aditya.supermarket.order.repository.OrderRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Creates customer-facing order numbers like {@code SM-20261030-A7K9QX}. The random part skips look-alike
 * characters (0/O, 1/I) so a number read out over the phone is not misheard.
 */
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private static final String PREFIX = "SM-";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SUFFIX_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final OrderRepository orderRepository;

    public String next() {
        String date = DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now(clock));
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = PREFIX + date + "-" + randomSuffix();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique order number");
    }

    private String randomSuffix() {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return suffix.toString();
    }
}
