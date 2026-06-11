package com.ubs.wma.aat.rampuppack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import com.ubs.wma.aat.rampuppack.domain.EmailLog;
import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailTemplate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmailLogRepositoryTest extends RepositoryTestSupport {

    @Autowired
    EmailLogRepository repository;

    @Autowired
    EmailTemplateRepository templateRepository;

    @Test
    void pendingPartRoundTripsMergedValuesArraysAndJson() {
        EmailLog saved = repository.save(pendingPart("LOG_REPO_RT")).block();

        EmailLog reloaded = repository.findById(saved.id()).block();

        assertThat(reloaded.status()).isEqualTo(EmailStatus.PENDING);
        assertThat(reloaded.aceIds()).containsExactly("ACE-1001", "ACE-1002");
        assertThat(reloaded.attachmentFileNames()).containsExactly("a.html", "b.html");
        assertThat(reloaded.mergeFields()).containsEntry("faName", "Alex Advisor");
        assertThat(reloaded.mergedSubject()).isEqualTo("Subject for Alex Advisor");
        assertThat(reloaded.mergedBody()).contains("<p>merged body</p>");
        assertThat(reloaded.attemptCount()).isZero();
        assertThat(reloaded.createdBy()).isEqualTo("system");
    }

    @Test
    void claimRetryableClaimsOnlyFailedRowsInsideTheRetryWindow() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        EmailLog failedRecently = saveFailed("LOG_REPO_RECENT", Instant.now().minus(1, ChronoUnit.HOURS));
        EmailLog failedLongAgo = saveFailed("LOG_REPO_OLD", Instant.now().minus(8, ChronoUnit.DAYS));

        List<EmailLog> claimed = repository.claimRetryable(cutoff, 100).collectList().block();
        List<Long> claimedIds = claimed.stream().map(EmailLog::id).toList();

        assertThat(claimedIds).contains(failedRecently.id());
        assertThat(claimedIds).doesNotContain(failedLongAgo.id());
        assertThat(claimed.stream().filter(row -> row.id().equals(failedRecently.id())))
                .allSatisfy(row -> assertThat(row.status()).isEqualTo(EmailStatus.SENDING));
    }

    @Test
    void markExhaustedClosesRowsWhoseWindowElapsed() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        EmailLog failedLongAgo = saveFailed("LOG_REPO_EXHAUST", Instant.now().minus(8, ChronoUnit.DAYS));

        List<EmailLog> exhausted = repository.markExhausted(cutoff).collectList().block();

        assertThat(exhausted.stream().map(EmailLog::id)).contains(failedLongAgo.id());
        assertThat(repository.findById(failedLongAgo.id()).block().status())
                .isEqualTo(EmailStatus.EXHAUSTED);
    }

    private EmailLog saveFailed(String templateCode, Instant firstAttemptedAt) {
        EmailLog pending = repository.save(pendingPart(templateCode)).block();
        return repository.save(pending.asFailed("sme-x", "delivery failed", firstAttemptedAt)).block();
    }

    private EmailLog pendingPart(String templateCode) {
        EmailTemplate template = templateRepository.save(EmailTemplate.newTemplate(
                templateCode, templateCode, null, "s", "b", true)).block();
        return EmailLog.pendingPart(null, template.id(), template.id(), "FA-1", null, "fa1@ubs.com",
                List.of("ACE-1001", "ACE-1002"), Map.of("faName", "Alex Advisor"),
                "Subject for Alex Advisor", "<p>merged body</p>", List.of("a.html", "b.html"), 1, 1);
    }
}
