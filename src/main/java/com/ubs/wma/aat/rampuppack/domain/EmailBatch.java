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
 * Scheduled send request mapped to the {@code email_batch} table — the same payload as a live
 * send plus {@code scheduledAt}. Due rows are claimed by the scheduler and run through the same
 * send pipeline as the live API; the per-part outcome lives in {@code email_log} (linked via
 * {@code email_log.batch_id}).
 */
@Table(schema = "aat_app", name = "email_batch")
public record EmailBatch(
        @Id Long id,
        @Column("fa_id") String faId,
        @Column("field_leader_id") String fieldLeaderId,
        @Column("recipient_email") String recipientEmail,
        @Column("template_id") Long templateId,
        @Column("fail_template_id") Long failTemplateId,
        @Column("ace_ids") List<String> aceIds,
        @Column("merge_fields") Map<String, String> mergeFields,
        @Column("scheduled_at") Instant scheduledAt,
        @Column("status") BatchStatus status,
        @Column("processed_at") Instant processedAt,
        @Column("failure_reason") String failureReason,
        @Column("created_by") @CreatedBy String createdBy,
        @Column("updated_by") @LastModifiedBy String updatedBy,
        @Column("created_at") @CreatedDate Instant createdAt,
        @Column("updated_at") @LastModifiedDate Instant updatedAt) {

    /** A new, transient batch entry waiting for its schedule time. */
    public static EmailBatch scheduled(
            String faId,
            String fieldLeaderId,
            String recipientEmail,
            Long templateId,
            Long failTemplateId,
            List<String> aceIds,
            Map<String, String> mergeFields,
            Instant scheduledAt) {
        return new EmailBatch(
                null,
                faId,
                fieldLeaderId,
                recipientEmail,
                templateId,
                failTemplateId,
                List.copyOf(aceIds),
                Map.copyOf(mergeFields),
                scheduledAt,
                BatchStatus.SCHEDULED,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /** A copy marked with the processing outcome ({@code COMPLETED} or {@code FAILED}). */
    public EmailBatch asProcessed(BatchStatus outcome, String failureReason, Instant processedAt) {
        return new EmailBatch(
                id,
                faId,
                fieldLeaderId,
                recipientEmail,
                templateId,
                failTemplateId,
                aceIds,
                mergeFields,
                scheduledAt,
                outcome,
                processedAt,
                failureReason,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt);
    }

    /** A copy marked {@code CANCELLED} (only valid while still {@code SCHEDULED}). */
    public EmailBatch asCancelled() {
        return new EmailBatch(
                id,
                faId,
                fieldLeaderId,
                recipientEmail,
                templateId,
                failTemplateId,
                aceIds,
                mergeFields,
                scheduledAt,
                BatchStatus.CANCELLED,
                processedAt,
                failureReason,
                createdBy,
                updatedBy,
                createdAt,
                updatedAt);
    }
}
