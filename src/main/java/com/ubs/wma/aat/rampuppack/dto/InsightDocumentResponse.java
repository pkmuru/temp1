package com.ubs.wma.aat.rampuppack.dto;

import java.time.Instant;

/** Read-only view of a STAAT insight document (retention pack) for an ACE relationship. */
public record InsightDocumentResponse(
        Long id,
        String aceId,
        String leadClientName,
        String fileName,
        String htmlContent,
        Instant createdAt,
        Instant updatedAt) {
}
