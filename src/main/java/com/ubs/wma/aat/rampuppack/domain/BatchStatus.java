package com.ubs.wma.aat.rampuppack.domain;

/**
 * Lifecycle of an {@link EmailBatch} row (one scheduled send request).
 *
 * <p>{@code SCHEDULED} rows whose {@code scheduledAt} is due are claimed by the scheduler
 * ({@code PROCESSING}) and finish as {@code COMPLETED} or {@code FAILED}. A row can be
 * {@code CANCELLED} via the API while still {@code SCHEDULED}.
 */
public enum BatchStatus {
    SCHEDULED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
