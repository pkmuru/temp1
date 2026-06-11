package com.ubs.wma.aat.rampuppack.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.util.StringUtils;

/**
 * Live send request: deliver the STAAT retention packs for {@code aceIds} to one recipient — a
 * receiving FA ({@code faId}) or, for pended/unassigned relationships, the Field Leader
 * ({@code fieldLeaderId}); exactly one of the two must be set. {@code templateId} renders the
 * email, {@code failTemplateId} renders the failure-notification email if delivery fails.
 * {@code mergeFields} are caller-supplied <code>{placeholder}</code> values (e.g. {@code faName});
 * the service adds {@code packCount}, {@code householdTable}, {@code partNumber},
 * {@code totalParts} and (for failure notices) {@code failureReason}.
 */
public record EmailSendRequest(
        @NotEmpty List<@NotBlank String> aceIds,
        String faId,
        String fieldLeaderId,
        @NotNull Long templateId,
        @NotNull Long failTemplateId,
        @NotBlank @Email String recipientEmail,
        Map<String, String> mergeFields) {

    public EmailSendRequest {
        mergeFields = normalizeMergeFields(mergeFields);
    }

    @AssertTrue(message = "exactly one of faId or fieldLeaderId must be provided")
    @Schema(hidden = true)
    public boolean isRecipientIdentifierValid() {
        return StringUtils.hasText(faId) ^ StringUtils.hasText(fieldLeaderId);
    }

    /** Null map → empty; null values → empty string (a placeholder resolves to blank, not "null"). */
    static Map<String, String> normalizeMergeFields(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        fields.forEach((name, value) -> normalized.put(name, value == null ? "" : value));
        return Collections.unmodifiableMap(normalized);
    }
}
