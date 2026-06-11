package com.ubs.wma.aat.rampuppack;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.dto.EmailBatchRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailBatchResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailTemplateRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailTemplateResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Full-context end-to-end smoke test: WebFlux → service → R2DBC → PostgreSQL, plus OpenAPI,
 * actuator, validation, auditing and ProblemDetail wiring. Entra passwordless is disabled so the
 * DB comes from the Zonky embedded PostgreSQL (no Docker needed; see AbstractIntegrationTest).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RampUpPackServiceApplicationTests extends AbstractIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void openApiDocumentIsServed() {
        webTestClient
                .get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.info.title")
                .isEqualTo("Ramp-Up Pack Service API");
    }

    @Test
    void healthEndpointIsUp() {
        webTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    void createThenFetchTemplateRoundTrip() {
        EmailTemplateRequest request = new EmailTemplateRequest(
                "SMOKE_TEST",
                "Smoke Test Template",
                "created by the application smoke test",
                "Hello {faName}",
                "<p>Hi {faName}, {packCount} packs attached.</p>{householdTable}",
                null);

        EmailTemplateResponse created = webTestClient
                .post()
                .uri("/api/v1/email-templates")
                .header("X-User-Id", "smoke-tester")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(EmailTemplateResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.active()).isTrue();
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.createdBy()).isEqualTo("smoke-tester");

        webTestClient
                .get()
                .uri("/api/v1/email-templates/{id}", created.id())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(EmailTemplateResponse.class)
                .value(template -> assertThat(template.code()).isEqualTo("SMOKE_TEST"));
    }

    @Test
    void addToBatchRoundTrip() {
        // The JSONB (merge fields) and TEXT[] (ace ids) mappings only exist on the batch/log
        // tables, so this round-trip proves the custom R2DBC converters end-to-end.
        EmailTemplateRequest template = new EmailTemplateRequest(
                "BATCH_RT_MAIN", "Batch Round-Trip", null, "Hi {faName}", "<p>{householdTable}</p>", null);
        EmailTemplateRequest failTemplate = new EmailTemplateRequest(
                "BATCH_RT_FAIL", "Batch Round-Trip Fail", null, "Issue {faName}", "<p>{failureReason}</p>", null);
        Long templateId = createTemplate(template);
        Long failTemplateId = createTemplate(failTemplate);

        EmailBatchRequest request = new EmailBatchRequest(
                List.of("ACE-9001", "ACE-9002"),
                "FA-42",
                null,
                templateId,
                failTemplateId,
                "fa42@ubs.com",
                Map.of("faName", "Alex Advisor"),
                Instant.parse("2026-07-01T08:00:00Z"));

        EmailBatchResponse created = webTestClient
                .post()
                .uri("/api/v1/email-batches")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(EmailBatchResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.status()).isEqualTo(BatchStatus.SCHEDULED);

        webTestClient
                .get()
                .uri("/api/v1/email-batches/{id}", created.id())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(EmailBatchResponse.class)
                .value(batch -> {
                    assertThat(batch.aceIds()).containsExactly("ACE-9001", "ACE-9002");
                    assertThat(batch.mergeFields()).containsEntry("faName", "Alex Advisor");
                });
    }

    private Long createTemplate(EmailTemplateRequest request) {
        EmailTemplateResponse response = webTestClient
                .post()
                .uri("/api/v1/email-templates")
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(EmailTemplateResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(response).isNotNull();
        return response.id();
    }

    @Test
    void unknownIdReturnsNotFound() {
        webTestClient
                .get()
                .uri("/api/v1/email-templates/{id}", 9_999_999)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Resource not found");
    }

    @Test
    void invalidPayloadReturnsBadRequest() {
        webTestClient
                .post()
                .uri("/api/v1/email-templates")
                .bodyValue(new EmailTemplateRequest("", null, null, "", "", null))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Validation failed");
    }
}
