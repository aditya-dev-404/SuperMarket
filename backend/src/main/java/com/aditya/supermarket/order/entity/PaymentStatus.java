package com.aditya.supermarket.order.entity;

/** Payment state of a whole order. */
public enum PaymentStatus {
    /** Online order waiting for the customer to pay. */
    PENDING_PAYMENT,
    PAID,
    /** The online payment failed; the order was cancelled and its stock returned. */
    PAYMENT_FAILED,
    /** Cash on delivery: nothing is collected until the parcel is delivered. */
    COD,
    /** A paid order was cancelled; the money still has to be returned to the customer. */
    REFUND_PENDING,
    /** Cancelled before any money was collected. */
    CANCELLED
}
