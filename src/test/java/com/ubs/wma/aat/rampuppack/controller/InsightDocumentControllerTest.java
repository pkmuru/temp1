package com.ubs.wma.aat.rampuppack.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import com.ubs.wma.aat.rampuppack.TestData;
import com.ubs.wma.aat.rampuppack.exception.ResourceNotFoundException;
import com.ubs.wma.aat.rampuppack.service.InsightDocumentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Web-layer slice test for {@link InsightDocumentController}: the read-only retrieval contract —
 * single document by path, multiple by the required {@code aceIds} query parameter, 404 mapping.
 */
@WebFluxTest(InsightDocumentController.class)
class InsightDocumentControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    InsightDocumentService service;

    @Test
    void getByAceIdReturnsTheDocumentIncludingHtml() {
        given(service.findByAceId("ACE-1001"))
                .willReturn(Mono.just(TestData.insightDocument("ACE-1001", "John Smith Household")));

        webTestClient.get().uri("/api/v1/insight-documents/ACE-1001")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.aceId").isEqualTo("ACE-1001")
                .jsonPath("$.leadClientName").isEqualTo("John Smith Household")
                .jsonPath("$.fileName").isEqualTo("John_Smith_Household.html")
                .jsonPath("$.htmlContent").isEqualTo("<html><body>pack</body></html>");
    }

    @Test
    void unknownAceIdReturns404ProblemDetail() {
        given(service.findByAceId("ACE-NOPE")).willReturn(
                Mono.error(new ResourceNotFoundException("STAAT insight document for ACE id", "ACE-NOPE")));

        webTestClient.get().uri("/api/v1/insight-documents/ACE-NOPE")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Resource not found");
    }

    @Test
    void listByAceIdsBindsTheCommaSeparatedParameter() {
        given(service.findByAceIds(List.of("ACE-1001", "ACE-1002"))).willReturn(Flux.just(
                TestData.insightDocument("ACE-1001", "John Smith Household"),
                TestData.insightDocument("ACE-1002", "Jane Doe Household")));

        webTestClient.get().uri("/api/v1/insight-documents?aceIds=ACE-1001,ACE-1002")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].aceId").isEqualTo("ACE-1001")
                .jsonPath("$[1].aceId").isEqualTo("ACE-1002");
    }

    @Test
    void listWithoutTheRequiredAceIdsParameterReturns400() {
        webTestClient.get().uri("/api/v1/insight-documents")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Bad request");

        then(service).shouldHaveNoInteractions();
    }
}
