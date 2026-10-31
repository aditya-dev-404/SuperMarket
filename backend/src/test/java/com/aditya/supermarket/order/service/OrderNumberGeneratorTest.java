package com.aditya.supermarket.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aditya.supermarket.order.repository.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderNumberGeneratorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private OrderRepository orderRepository;

    private OrderNumberGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new OrderNumberGenerator(CLOCK, orderRepository);
    }

    @Test
    void next_hasPrefixDateAndSixSafeCharacters() {
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);

        assertThat(generator.next()).matches("^SM-20261030-[A-HJ-NP-Z2-9]{6}$");
    }

    @Test
    void next_triesAgain_whenTheNumberIsTaken() {
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(true, false);

        assertThat(generator.next()).startsWith("SM-20261030-");
        verify(orderRepository, times(2)).existsByOrderNumber(anyString());
    }

    @Test
    void next_givesUp_afterFiveTakenNumbers() {
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(true);

        assertThatThrownBy(generator::next).isInstanceOf(IllegalStateException.class);
        verify(orderRepository, times(5)).existsByOrderNumber(anyString());
    }
}
