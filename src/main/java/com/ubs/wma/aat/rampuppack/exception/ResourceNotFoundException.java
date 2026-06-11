package com.ubs.wma.aat.rampuppack.exception;

/**
 * Thrown when a requested resource does not exist; mapped to HTTP 404 by
 * {@code GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s with id %s was not found".formatted(resource, id));
    }
}
