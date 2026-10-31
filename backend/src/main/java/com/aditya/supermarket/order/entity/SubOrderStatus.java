package com.aditya.supermarket.order.entity;

/**
 * Fulfilment state of one vendor's part of an order. The declaration order matters: it is the
 * order of progress, and the least advanced active sub-order decides the status of the whole order.
 */
public enum SubOrderStatus {
    /** Online order not paid yet; the vendor cannot see or act on it. */
    PENDING_PAYMENT,
    PLACED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
