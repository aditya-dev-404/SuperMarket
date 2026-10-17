package com.aditya.supermarket.common.exception;

/** Thrown when a request is well-formed but violates a business rule (maps to HTTP 422). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
