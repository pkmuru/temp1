package com.ubs.wma.aat.rampuppack.domain;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * STAAT retention-pack document mapped to the {@code staat_insight_document} table.
 *
 * <p><strong>Read-only for this service:</strong> the table lives in the externally owned
 * {@code datamesh} schema and is written by DS pipelines — one fully formed HTML document per ACE
 * relationship, identified to users by the lead client name (never the household id). This service
 * only retrieves and attaches them (SELECT-only grant in real environments).
 */
@Table(schema = "datamesh", name = "staat_insight_document")
public record StaatInsightDocument(
        @Id Long id,
        @Column("ace_id") String aceId,
        @Column("lead_client_name") String leadClientName,
        @Column("file_name") String fileName,
        @Column("html_content") String htmlContent,
        @Column("created_at") Instant createdAt,
        @Column("updated_at") Instant updatedAt) {

    /** Attachment size of this document, used for the per-email size cap (35 MB Outlook limit). */
    public long contentSizeBytes() {
        return htmlContent.getBytes(StandardCharsets.UTF_8).length;
    }
}
