package com.ubs.wma.aat.rampuppack.dto;

import java.time.Instant;

import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response representation of a ramp-up pack.
 */
@Schema(name = "RampUpPackResponse", description = "A ramp-up pack")
public record RampUpPackResponse(
        Long id,
        String name,
        String description,
        RampUpPackStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
