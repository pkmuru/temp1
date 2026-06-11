package com.ubs.wma.aat.rampuppack.dto;

import java.util.List;

/** One previewed email part: the merged subject and HTML body exactly as they would be sent. */
public record EmailPreviewPart(
        int partNumber,
        int totalParts,
        String subject,
        String htmlContent,
        List<String> aceIds,
        List<String> attachmentFileNames) {
}
