package com.ubs.wma.aat.rampuppack.exception;

/**
 * Thrown when a request conflicts with the current state of a resource (e.g. cancelling a batch
 * that already started processing, or sending with an inactive template); mapped to HTTP 409 by
 * {@code GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
