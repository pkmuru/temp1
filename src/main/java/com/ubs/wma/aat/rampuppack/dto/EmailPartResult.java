package com.ubs.wma.aat.rampuppack.dto;

import java.util.List;

import com.ubs.wma.aat.rampuppack.domain.EmailStatus;

/** Outcome of one email part of a send (one {@code email_log} row). */
public record EmailPartResult(
        Long emailLogId,
        int partNumber,
        int totalParts,
        EmailStatus status,
        String smeId,
        String failureReason,
        List<String> attachmentFileNames) {
}
