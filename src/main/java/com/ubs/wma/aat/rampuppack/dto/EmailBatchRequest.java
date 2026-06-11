package com.ubs.wma.aat.rampuppack.dto;

import java.time.Instant;
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
 * Add-to-batch request — the same payload as {@link EmailSendRequest} plus {@code scheduledAt},
 * the date/time from which the scheduler may pick the entry up (a past value is processed on the
 * next poll).
 */
public record EmailBatchRequest(
        @NotEmpty List<@NotBlank String> aceIds,
        String faId,
        String fieldLeaderId,
        @NotNull Long templateId,
        @NotNull Long failTemplateId,
        @NotBlank @Email String recipientEmail,
        Map<String, String> mergeFields,
        @NotNull Instant scheduledAt) {

    public EmailBatchRequest {
        mergeFields = EmailSendRequest.normalizeMergeFields(mergeFields);
    }

    @AssertTrue(message = "exactly one of faId or fieldLeaderId must be provided")
    @Schema(hidden = true)
    public boolean isRecipientIdentifierValid() {
        return StringUtils.hasText(faId) ^ StringUtils.hasText(fieldLeaderId);
    }
}
