package com.aditya.supermarket.inventory.service;

import com.aditya.supermarket.common.exception.BusinessRuleException;
import com.aditya.supermarket.inventory.repository.StockRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Takes stock when an order is placed and gives it back when the order is cancelled or its payment
 * fails. Lines are always processed in variant-id order so two checkouts touching the same variants
 * lock them in the same order and cannot deadlock each other.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Comparator<StockLine> BY_VARIANT = Comparator.comparing(StockLine::variantId);

    private final StockRepository stockRepository;

    /** All or nothing: the caller's transaction rolls back every earlier deduction when one line fails. */
    @Transactional
    public void deduct(List<StockLine> lines) {
        lines.stream().sorted(BY_VARIANT).forEach(line -> {
            if (stockRepository.decrease(line.variantId(), line.quantity()) == 0) {
                throw new BusinessRuleException("'" + line.description()
                    + "' just went out of stock or has less stock than you asked for. Please review your cart.");
            }
        });
    }

    @Transactional
    public void restore(List<StockLine> lines) {
        lines.stream()
            .filter(line -> line.variantId() != null)
            .sorted(BY_VARIANT)
            .forEach(line -> stockRepository.increase(line.variantId(), line.quantity()));
    }
}
