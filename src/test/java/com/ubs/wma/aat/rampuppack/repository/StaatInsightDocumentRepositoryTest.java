package com.ubs.wma.aat.rampuppack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubs.wma.aat.rampuppack.domain.StaatInsightDocument;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

/**
 * Reads against the externally populated {@code datamesh.staat_insight_document} table —
 * the rows come from {@code db/seed.sql}, exactly like datamesh would provide them.
 */
class StaatInsightDocumentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    StaatInsightDocumentRepository repository;

    @Test
    void findsSeededDocumentByAceId() {
        StepVerifier.create(repository.findByAceId("ACE-1001"))
                .assertNext(document -> {
                    assertThat(document.leadClientName()).isEqualTo("John Smith Household");
                    assertThat(document.fileName()).isEqualTo("John_Smith_Household.html");
                    assertThat(document.htmlContent()).contains("<h1>John Smith Household</h1>");
                })
                .verifyComplete();
    }

    @Test
    void findsOnlyExistingDocumentsForMultipleAceIds() {
        List<String> found = repository
                .findByAceIdIn(List.of("ACE-1001", "ACE-1002", "ACE-UNKNOWN"))
                .map(StaatInsightDocument::aceId)
                .collectList()
                .block();

        assertThat(found).containsExactlyInAnyOrder("ACE-1001", "ACE-1002");
    }

    @Test
    void unknownAceIdYieldsEmpty() {
        StepVerifier.create(repository.findByAceId("ACE-UNKNOWN")).verifyComplete();
    }
}
