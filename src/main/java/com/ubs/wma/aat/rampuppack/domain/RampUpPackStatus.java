package com.ubs.wma.aat.rampuppack.domain;

/**
 * Lifecycle status of a ramp-up pack. Stored in the {@code status} column as its {@link #name()}
 * (see the converters in {@code R2dbcConfig}).
 */
public enum RampUpPackStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
