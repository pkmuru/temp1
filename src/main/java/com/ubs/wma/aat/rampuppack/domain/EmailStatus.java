package com.ubs.wma.aat.rampuppack.domain;

/**
 * Lifecycle of an {@link EmailLog} row (one email part).
 *
 * <p>{@code PENDING} → {@code SENDING} → {@code SENT}, or {@code FAILED} on a delivery error.
 * {@code FAILED} rows are re-claimed ({@code SENDING}) and retried by the scheduler until the
 * retry window (7 days from the first attempt) elapses, after which they become {@code EXHAUSTED}.
 */
public enum EmailStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
    EXHAUSTED
}
