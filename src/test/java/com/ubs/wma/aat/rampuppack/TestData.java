package com.ubs.wma.aat.rampuppack;

import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailBatch;
import com.ubs.wma.aat.rampuppack.domain.EmailLog;
import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailTemplate;
import com.ubs.wma.aat.rampuppack.domain.StaatInsightDocument;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Canonical domain fixtures for tests that need fully populated entities without a database
 * (web-slice and unit tests). Timestamps are fixed so assertions stay deterministic. Tests that
 * persist rows (repository slices, end-to-end) should keep creating their own data with unique
 * marker values instead — the embedded PostgreSQL is shared across contexts.
 */
public final class TestData {

    /** Fixed "now" used by all fixtures. */
    public static final Instant NOW = Instant.parse("2026-06-11T10:00:00Z");

    /** Fixed schedule time used by batch fixtures. */
    public static final Instant SCHEDULED_AT = Instant.parse("2026-07-01T08:00:00Z");

    private TestData() {}

    /** An active, persisted-looking template with {@code {faName}} in the body. */
    public static EmailTemplate template(Long id, String code) {
        return new EmailTemplate(
                id,
                code,
                "Successful Delivery - FA",
                "FA success email",
                "STAAT Client Retention Packs",
                "<p>Hi {faName}</p>",
                true,
                "muru",
                "muru",
                NOW,
                NOW);
    }

    /** A single-part FA send that succeeded on its first attempt. */
    public static EmailLog sentLog() {
        return new EmailLog(
                42L,
                null,
                1L,
                2L,
                "FA-42",
                null,
                "fa42@ubs.com",
                List.of("ACE-1001"),
                Map.of("faName", "Alex Advisor"),
                "STAAT Client Retention Packs",
                "<p>Hi Alex Advisor</p>",
                List.of("John_Smith_Household.html"),
                1,
                1,
                false,
                "sme-abc",
                EmailStatus.SENT,
                1,
                NOW,
                NOW,
                null,
                "system",
                "system",
                NOW,
                NOW);
    }

    /** A persisted-looking Field-Leader batch entry in the given state. */
    public static EmailBatch batch(BatchStatus status) {
        return new EmailBatch(
                11L,
                null,
                "FL-7",
                "fl7@ubs.com",
                2L,
                3L,
                List.of("ACE-1003"),
                Map.of("fieldLeaderName", "Pat Leader"),
                SCHEDULED_AT,
                status,
                null,
                null,
                "muru",
                "muru",
                NOW,
                NOW);
    }

    /** A retention-pack document as datamesh would provide it. */
    public static StaatInsightDocument insightDocument(String aceId, String leadClientName) {
        return new StaatInsightDocument(
                1L,
                aceId,
                leadClientName,
                leadClientName.replace(' ', '_') + ".html",
                "<html><body>pack</body></html>",
                NOW,
                NOW);
    }
}
