package com.ubs.wma.aat.rampuppack;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;
import com.ubs.wma.aat.rampuppack.dto.RampUpPackRequest;
import com.ubs.wma.aat.rampuppack.dto.RampUpPackResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Full-context end-to-end test: WebFlux → service → R2DBC → PostgreSQL, plus OpenAPI,
 * actuator, validation and ProblemDetail wiring. Entra passwordless is disabled so the DB
 * comes from the Zonky embedded PostgreSQL (no Docker needed; see AbstractIntegrationTest).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RampUpPackServiceApplicationTests extends AbstractIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void openApiDocumentIsServed() {
        webTestClient.get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.info.title").isEqualTo("Ramp-Up Pack Service API");
    }

    @Test
    void healthEndpointIsUp() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void createThenFetchRoundTrip() {
        RampUpPackRequest request = new RampUpPackRequest(
                "APAC Advisor Onboarding", "Pack for APAC advisors", RampUpPackStatus.DRAFT);

        RampUpPackResponse created = webTestClient.post().uri("/api/v1/ramp-up-packs")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RampUpPackResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(RampUpPackStatus.DRAFT);
        assertThat(created.createdAt()).isNotNull();

        webTestClient.get().uri("/api/v1/ramp-up-packs/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(RampUpPackResponse.class)
                .value(pack -> assertThat(pack.name()).isEqualTo("APAC Advisor Onboarding"));
    }

    @Test
    void unknownIdReturnsNotFound() {
        webTestClient.get().uri("/api/v1/ramp-up-packs/{id}", 9_999_999)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Resource not found");
    }

    @Test
    void invalidPayloadReturnsBadRequest() {
        webTestClient.post().uri("/api/v1/ramp-up-packs")
                .bodyValue(new RampUpPackRequest("", null, null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Validation failed");
    }
}
