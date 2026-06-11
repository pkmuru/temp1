package com.ubs.wma.aat.rampuppack.domain;

import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Email template mapped to the {@code email_template} table. {@code subject} and {@code body}
 * may contain <code>{placeholder}</code> merge fields resolved at send time (see
 * {@code TemplateMerger}). Audit columns are populated by Spring Data auditing — the
 * {@code created_by}/{@code updated_by} auditor comes from the {@code X-User-Id} request header
 * (default {@code system}), see {@code AuditingConfig}.
 */
@Table(schema = "aat_app", name = "email_template")
public record EmailTemplate(
        @Id Long id,
        @Column("code") String code,
        @Column("name") String name,
        @Column("description") String description,
        @Column("subject") String subject,
        @Column("body") String body,
        @Column("active") boolean active,
        @Column("created_by") @CreatedBy String createdBy,
        @Column("updated_by") @LastModifiedBy String updatedBy,
        @Column("created_at") @CreatedDate Instant createdAt,
        @Column("updated_at") @LastModifiedDate Instant updatedAt) {

    /** A new, transient template: no id and no audit values (assigned on persist). */
    public static EmailTemplate newTemplate(
            String code, String name, String description, String subject, String body, boolean active) {
        return new EmailTemplate(null, code, name, description, subject, body, active, null, null, null, null);
    }

    /** A copy with the editable fields replaced, preserving id and audit values. */
    public EmailTemplate withChanges(
            String code, String name, String description, String subject, String body, boolean active) {
        return new EmailTemplate(
                this.id,
                code,
                name,
                description,
                subject,
                body,
                active,
                this.createdBy,
                this.updatedBy,
                this.createdAt,
                this.updatedAt);
    }
}
