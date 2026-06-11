package com.ubs.wma.aat.rampuppack.mapper;

import com.ubs.wma.aat.rampuppack.domain.StaatInsightDocument;
import com.ubs.wma.aat.rampuppack.dto.InsightDocumentResponse;

/** Stateless mapping from {@link StaatInsightDocument} to its read-only API DTO. */
public final class InsightDocumentMapper {

    private InsightDocumentMapper() {
    }

    public static InsightDocumentResponse toResponse(StaatInsightDocument document) {
        return new InsightDocumentResponse(document.id(), document.aceId(), document.leadClientName(),
                document.fileName(), document.htmlContent(), document.createdAt(), document.updatedAt());
    }
}
