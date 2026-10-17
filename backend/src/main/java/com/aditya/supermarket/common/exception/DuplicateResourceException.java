package com.aditya.supermarket.common.exception;

/** Thrown when creating a resource that already exists (maps to HTTP 409). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
