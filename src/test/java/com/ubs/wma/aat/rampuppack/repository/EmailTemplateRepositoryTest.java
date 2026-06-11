package com.ubs.wma.aat.rampuppack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubs.wma.aat.rampuppack.domain.EmailTemplate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import reactor.test.StepVerifier;

class EmailTemplateRepositoryTest extends RepositoryTestSupport {

    @Autowired
    EmailTemplateRepository repository;

    @Test
    void saveAssignsIdAndAuditColumns() {
        EmailTemplate template = EmailTemplate.newTemplate("REPO_TPL_AUDIT", "Audit Test", null,
                "Subject {faName}", "<p>{householdTable}</p>", true);

        StepVerifier.create(repository.save(template))
                .assertNext(saved -> {
                    assertThat(saved.id()).isNotNull();
                    // Populated by Spring Data auditing on insert ('system' outside a web request).
                    assertThat(saved.createdAt()).isNotNull();
                    assertThat(saved.updatedAt()).isNotNull();
                    assertThat(saved.createdBy()).isEqualTo("system");
                    assertThat(saved.updatedBy()).isEqualTo("system");
                })
                .verifyComplete();
    }

    @Test
    void findByActiveFilters() {
        EmailTemplate active = repository.save(EmailTemplate.newTemplate(
                "REPO_TPL_ACTIVE", "Active", null, "s", "b", true)).block();
        EmailTemplate inactive = repository.save(EmailTemplate.newTemplate(
                "REPO_TPL_INACTIVE", "Inactive", null, "s", "b", false)).block();

        var activeCodes = repository.findByActive(true).map(EmailTemplate::code).collectList().block();
        var inactiveCodes = repository.findByActive(false).map(EmailTemplate::code).collectList().block();

        assertThat(activeCodes).contains(active.code()).doesNotContain(inactive.code());
        assertThat(inactiveCodes).contains(inactive.code()).doesNotContain(active.code());
    }

    @Test
    void duplicateCodeIsRejectedByUniqueConstraint() {
        repository.save(EmailTemplate.newTemplate("REPO_TPL_DUP", "First", null, "s", "b", true)).block();

        StepVerifier.create(repository.save(
                        EmailTemplate.newTemplate("REPO_TPL_DUP", "Second", null, "s", "b", true)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }
}
