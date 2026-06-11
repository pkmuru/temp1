package com.ubs.wma.aat.rampuppack.dto;

import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Full view of a sent/attempted email part, including the merged values kept for reference. */
public record EmailLogResponse(
        Long id,
        Long batchId,
        Long templateId,
        Long failTemplateId,
        String faId,
        String fieldLeaderId,
        String recipientEmail,
        List<String> aceIds,
        Map<String, String> mergeFields,
        String mergedSubject,
        String mergedBody,
        List<String> attachmentFileNames,
        int partNumber,
        int totalParts,
        boolean failureNotification,
        String smeId,
        EmailStatus status,
        int attemptCount,
        Instant firstAttemptedAt,
        Instant lastAttemptedAt,
        String failureReason,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {}
