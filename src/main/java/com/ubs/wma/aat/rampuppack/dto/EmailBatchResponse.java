package com.ubs.wma.aat.rampuppack.dto;

import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record EmailBatchResponse(
        Long id,
        String faId,
        String fieldLeaderId,
        String recipientEmail,
        Long templateId,
        Long failTemplateId,
        List<String> aceIds,
        Map<String, String> mergeFields,
        Instant scheduledAt,
        BatchStatus status,
        Instant processedAt,
        String failureReason,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt) {}
