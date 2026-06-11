package com.ubs.wma.aat.rampuppack.mapper;

import java.util.List;

import com.ubs.wma.aat.rampuppack.domain.EmailBatch;
import com.ubs.wma.aat.rampuppack.domain.EmailLog;
import com.ubs.wma.aat.rampuppack.dto.EmailBatchRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailBatchResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailLogResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailPartResult;
import com.ubs.wma.aat.rampuppack.dto.EmailPreviewPart;
import com.ubs.wma.aat.rampuppack.dto.EmailPreviewResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailSendRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailSendResponse;

/** Stateless mapping for the send/batch/log API DTOs. */
public final class EmailMapper {

    private EmailMapper() {
    }

    public static EmailBatch toEntity(EmailBatchRequest request) {
        return EmailBatch.scheduled(request.faId(), request.fieldLeaderId(), request.recipientEmail(),
                request.templateId(), request.failTemplateId(), request.aceIds(),
                request.mergeFields(), request.scheduledAt());
    }

    /** The live-send payload equivalent of a claimed batch row (scheduler → send pipeline). */
    public static EmailSendRequest toSendRequest(EmailBatch batch) {
        return new EmailSendRequest(batch.aceIds(), batch.faId(), batch.fieldLeaderId(),
                batch.templateId(), batch.failTemplateId(), batch.recipientEmail(),
                batch.mergeFields());
    }

    public static EmailBatchResponse toResponse(EmailBatch batch) {
        return new EmailBatchResponse(batch.id(), batch.faId(), batch.fieldLeaderId(),
                batch.recipientEmail(), batch.templateId(), batch.failTemplateId(), batch.aceIds(),
                batch.mergeFields(), batch.scheduledAt(), batch.status(), batch.processedAt(),
                batch.failureReason(), batch.createdBy(), batch.updatedBy(), batch.createdAt(),
                batch.updatedAt());
    }

    public static EmailSendResponse toSendResponse(List<EmailLog> parts) {
        return new EmailSendResponse(parts.stream().map(EmailMapper::toPartResult).toList());
    }

    public static EmailPartResult toPartResult(EmailLog emailLog) {
        return new EmailPartResult(emailLog.id(), emailLog.partNumber(), emailLog.totalParts(),
                emailLog.status(), emailLog.smeId(), emailLog.failureReason(),
                emailLog.attachmentFileNames());
    }

    /** Preview view of planned (unsaved, unsent) part rows: merged subject + HTML body per part. */
    public static EmailPreviewResponse toPreviewResponse(String recipientEmail, List<EmailLog> plannedRows) {
        List<EmailPreviewPart> parts = plannedRows.stream()
                .map(row -> new EmailPreviewPart(row.partNumber(), row.totalParts(),
                        row.mergedSubject(), row.mergedBody(), row.aceIds(), row.attachmentFileNames()))
                .toList();
        return new EmailPreviewResponse(recipientEmail, parts);
    }

    public static EmailLogResponse toResponse(EmailLog emailLog) {
        return new EmailLogResponse(emailLog.id(), emailLog.batchId(), emailLog.templateId(),
                emailLog.failTemplateId(), emailLog.faId(), emailLog.fieldLeaderId(),
                emailLog.recipientEmail(), emailLog.aceIds(), emailLog.mergeFields(),
                emailLog.mergedSubject(), emailLog.mergedBody(), emailLog.attachmentFileNames(),
                emailLog.partNumber(), emailLog.totalParts(), emailLog.failureNotification(),
                emailLog.smeId(), emailLog.status(), emailLog.attemptCount(),
                emailLog.firstAttemptedAt(), emailLog.lastAttemptedAt(), emailLog.failureReason(),
                emailLog.createdBy(), emailLog.createdAt(), emailLog.updatedAt());
    }
}
