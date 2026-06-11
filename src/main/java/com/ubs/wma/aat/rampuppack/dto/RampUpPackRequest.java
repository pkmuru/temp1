package com.ubs.wma.aat.rampuppack.dto;

import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating a ramp-up pack.
 */
@Schema(name = "RampUpPackRequest", description = "Payload to create or update a ramp-up pack")
public record RampUpPackRequest(

        @Schema(description = "Display name", example = "APAC Advisor Onboarding", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 200)
        String name,

        @Schema(description = "Free-text description", example = "Onboarding materials for new APAC advisors")
        @Size(max = 2000)
        String description,

        @Schema(description = "Lifecycle status; defaults to DRAFT when omitted", example = "DRAFT")
        RampUpPackStatus status) {
}
