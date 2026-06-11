package com.ubs.wma.aat.rampuppack.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Send record mapped to the {@code email_log} table — one row per email <em>part</em> actually
 * attempted (a logical send splits into parts when the attachments exceed the per-email size cap).
 *
 * <p>The merged subject/body are stored verbatim for future reference and reused as-is on retry;
 * attachments are re-fetched by {@code aceIds} (the subset attached to this part) and re-uploaded.
 * Failure-notification emails are logged as their own rows with {@code failureNotification=true}
 * and no {@code failTemplateId}, so an exhausted notification never triggers another notification.
 */
@Table(schema = "aat_app", name = "email_log")
public record EmailLog(
        @Id Long id,
        @Column("batch_id") Long batchId,
        @Column("template_id") Long templateId,
        @Column("fail_template_id") Long failTemplateId,
        @Column("fa_id") String faId,
        @Column("field_leader_id") String fieldLeaderId,
        @Column("recipient_email") String recipientEmail,
        @Column("ace_ids") List<String> aceIds,
        @Column("merge_fields") Map<String, String> mergeFields,
        @Column("merged_subject") String mergedSubject,
        @Column("merged_body") String mergedBody,
        @Column("attachment_file_names") List<String> attachmentFileNames,
        @Column("part_number") int partNumber,
        @Column("total_parts") int totalParts,
        @Column("failure_notification") boolean failureNotification,
        @Column("sme_id") String smeId,
        @Column("status") EmailStatus status,
        @Column("attempt_count") int attemptCount,
        @Column("first_attempted_at") Instant firstAttemptedAt,
        @Column("last_attempted_at") Instant lastAttemptedAt,
        @Column("failure_reason") String failureReason,
        @Column("created_by") @CreatedBy String createdBy,
        @Column("updated_by") @LastModifiedBy String updatedBy,
        @Column("created_at") @CreatedDate Instant createdAt,
        @Column("updated_at") @LastModifiedDate Instant updatedAt) {

    /** A new, transient part row awaiting its first delivery attempt. */
    public static EmailLog pendingPart(
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
            int totalParts) {
        return new EmailLog(
                null,
                batchId,
                templateId,
                failTemplateId,
                faId,
                fieldLeaderId,
                recipientEmail,
                List.copyOf(aceIds),
                Map.copyOf(mergeFields),
                mergedSubject,
                mergedBody,
                List.copyOf(attachmentFileNames),
                partNumber,
                totalParts,
                false,
                null,
                EmailStatus.PENDING,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * A new, transient failure-notification row derived from a failed part: same recipient and
     * ACE context, no attachments, and {@code failTemplateId=null} so it never notifies again.
     */
    public static EmailLog failureNotice(
            EmailLog failedPart, Map<String, String> mergeFields, String mergedSubject, String mergedBody) {
        return new EmailLog(
                null,
                failedPart.batchId(),
                failedPart.failTemplateId(),
                null,
                failedPart.faId(),
                failedPart.fieldLeaderId(),
                failedPart.recipientEmail(),
                failedPart.aceIds(),
                Map.copyOf(mergeFields),
                mergedSubject,
                mergedBody,
                List.of(),
                1,
                1,
                true,
                null,
                EmailStatus.PENDING,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /** A copy recording a successful delivery attempt. */
    public EmailLog asSent(String smeId, Instant attemptedAt) {
        return attempted(EmailStatus.SENT, smeId, null, attemptedAt);
    }

    /** A copy recording a failed delivery attempt (eligible for retry by the scheduler). */
    public EmailLog asFailed(String smeId, String failureReason, Instant attemptedAt) {
        return attempted(EmailStatus.FAILED, smeId, failureReason, attemptedAt);
    }

    private EmailLog attempted(EmailStatus outcome, String smeId, String failureReason, Instant attemptedAt) {
        return new EmailLog(
                id,
                batchId,
                templateId,
                failTemplateId,
                faId,
                fieldLeaderId,
                recipientEmail,
                aceIds,
                mergeFields,
                mergedSubject,
                mergedBody,
                attachmentFileNames,
                partNumber,
                totalParts,
                failureNotification,
                smeId,
                outcome,
                attemptCount + 1,
                firstAttemptedAt != null ? firstAttemptedAt : attemptedAt,
                attemptedAt,
                failureReason,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt);
    }
}
