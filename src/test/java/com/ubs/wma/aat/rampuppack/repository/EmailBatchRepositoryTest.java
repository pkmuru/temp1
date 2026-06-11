package com.ubs.wma.aat.rampuppack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailBatch;
import com.ubs.wma.aat.rampuppack.domain.EmailTemplate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmailBatchRepositoryTest extends RepositoryTestSupport {

    @Autowired
    EmailBatchRepository repository;

    @Autowired
    EmailTemplateRepository templateRepository;

    @Test
    void jsonbMergeFieldsAndAceIdArrayRoundTrip() {
        EmailBatch saved = repository
                .save(newBatch("BATCH_REPO_RT", Instant.now().plus(1, ChronoUnit.DAYS)))
                .block();

        EmailBatch reloaded = repository.findById(saved.id()).block();

        assertThat(reloaded.aceIds()).containsExactly("ACE-1001", "ACE-1002");
        assertThat(reloaded.mergeFields())
                .containsEntry("faName", "Alex Advisor")
                .containsEntry("FA Name", "Alex Advisor");
        assertThat(reloaded.status()).isEqualTo(BatchStatus.SCHEDULED);
        assertThat(reloaded.createdBy()).isEqualTo("system");
    }

    @Test
    void claimDueClaimsOnlyDueScheduledRows() {
        EmailBatch due = repository
                .save(newBatch("BATCH_REPO_DUE", Instant.now().minus(1, ChronoUnit.HOURS)))
                .block();
        EmailBatch future = repository
                .save(newBatch("BATCH_REPO_FUTURE", Instant.now().plus(1, ChronoUnit.DAYS)))
                .block();
        EmailBatch cancelled = repository
                .save(repository
                        .save(newBatch("BATCH_REPO_CANCELLED", Instant.now().minus(1, ChronoUnit.HOURS)))
                        .block()
                        .asCancelled())
                .block();

        List<EmailBatch> claimed = repository.claimDue(100).collectList().block();
        List<Long> claimedIds = claimed.stream().map(EmailBatch::id).toList();

        assertThat(claimedIds).contains(due.id());
        assertThat(claimedIds).doesNotContain(future.id(), cancelled.id());
        assertThat(claimed.stream().filter(batch -> batch.id().equals(due.id())))
                .allSatisfy(batch -> assertThat(batch.status()).isEqualTo(BatchStatus.PROCESSING));

        // A second poll must not hand the same row out again — it is already PROCESSING.
        List<Long> secondClaim =
                repository.claimDue(100).map(EmailBatch::id).collectList().block();
        assertThat(secondClaim).doesNotContain(due.id());
    }

    private EmailBatch newBatch(String templateCode, Instant scheduledAt) {
        EmailTemplate template = templateRepository
                .save(EmailTemplate.newTemplate(templateCode, templateCode, null, "s", "b", true))
                .block();
        return EmailBatch.scheduled(
                "FA-1",
                null,
                "fa1@ubs.com",
                template.id(),
                template.id(),
                List.of("ACE-1001", "ACE-1002"),
                Map.of("faName", "Alex Advisor", "FA Name", "Alex Advisor"),
                scheduledAt);
    }
}
