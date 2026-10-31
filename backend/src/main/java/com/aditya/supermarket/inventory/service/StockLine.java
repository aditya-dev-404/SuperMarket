package com.aditya.supermarket.inventory.service;

/**
 * A quantity of one variant to take from or return to stock.
 *
 * @param variantId   null when the vendor has since deleted the variant (nothing to restore then)
 * @param description shown to the customer when there is not enough stock
 */
public record StockLine(Long variantId, int quantity, String description) {
}
