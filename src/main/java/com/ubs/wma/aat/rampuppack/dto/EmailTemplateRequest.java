package com.ubs.wma.aat.rampuppack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for an email template. {@code subject} and {@code body} may contain
 * <code>{placeholder}</code> merge fields; {@code active} defaults to {@code true} when omitted.
 */
public record EmailTemplateRequest(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 500) String subject,
        @NotBlank String body,
        Boolean active) {

    public boolean activeOrDefault() {
        return active == null || active;
    }
}
