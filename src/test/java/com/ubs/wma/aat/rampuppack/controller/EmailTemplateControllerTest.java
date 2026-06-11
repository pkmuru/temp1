package com.ubs.wma.aat.rampuppack.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.stream.Stream;

import com.ubs.wma.aat.rampuppack.TestData;
import com.ubs.wma.aat.rampuppack.exception.ResourceNotFoundException;
import com.ubs.wma.aat.rampuppack.service.EmailTemplateService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Web-layer slice test for {@link EmailTemplateController}: routing, status codes, bean
 * validation, JSON shape and {@code GlobalExceptionHandler} mapping — the service is mocked,
 * business behaviour is covered by {@code EmailDeliveryFlowTest} and the repository tests.
 */
@WebFluxTest(EmailTemplateController.class)
class EmailTemplateControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    EmailTemplateService service;

    @Test
    void listReturnsTemplatesAsJson() {
        given(service.findAll(null)).willReturn(Flux.just(TestData.template(1L, "FA_DELIVERY_SUCCESS")));

        webTestClient.get().uri("/api/v1/email-templates")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[0].code").isEqualTo("FA_DELIVERY_SUCCESS")
                .jsonPath("$[0].subject").isEqualTo("STAAT Client Retention Packs")
                .jsonPath("$[0].active").isEqualTo(true)
                .jsonPath("$[0].createdBy").isEqualTo("muru");
    }

    @Test
    void listForwardsTheActiveFilter() {
        given(service.findAll(true)).willReturn(Flux.empty());

        webTestClient.get().uri("/api/v1/email-templates?active=true")
                .exchange()
                .expectStatus().isOk();

        then(service).should().findAll(true);
    }

    @Test
    void getUnknownIdReturns404ProblemDetail() {
        given(service.findById(99L)).willReturn(Mono.error(new ResourceNotFoundException("Email template", 99L)));

        webTestClient.get().uri("/api/v1/email-templates/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Resource not found")
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("Email template with id 99 was not found");
    }

    @Test
    void createReturns201WithBody() {
        given(service.create(any())).willReturn(Mono.just(TestData.template(5L, "NEW_CODE")));

        webTestClient.post().uri("/api/v1/email-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"code":"NEW_CODE","name":"New","subject":"Subject","body":"<p>{x}</p>"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.code").isEqualTo("NEW_CODE");
    }

    static Stream<Arguments> invalidTemplateBodies() {
        return Stream.of(
                arguments("blank code",
                        """
                        {"code":"","name":"New","subject":"Subject","body":"<p>x</p>"}
                        """,
                        "code"),
                arguments("blank subject",
                        """
                        {"code":"CODE","name":"New","subject":"","body":"<p>x</p>"}
                        """,
                        "subject"),
                arguments("missing body",
                        """
                        {"code":"CODE","name":"New","subject":"Subject"}
                        """,
                        "body"),
                arguments("code over the 100-character limit",
                        """
                        {"code":"%s","name":"New","subject":"Subject","body":"<p>x</p>"}
                        """.formatted("X".repeat(101)),
                        "code"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTemplateBodies")
    void invalidCreateRequestsAreRejectedWithoutReachingTheService(String description, String body,
                                                                   String expectedInDetail) {
        webTestClient.post().uri("/api/v1/email-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Validation failed")
                .jsonPath("$.detail").value(detail ->
                        assertThat(detail.toString()).contains(expectedInDetail));

        then(service).shouldHaveNoInteractions();
    }

    @Test
    void duplicateCodeMapsTo409DataConflict() {
        given(service.create(any())).willReturn(
                Mono.error(new DataIntegrityViolationException("duplicate key email_template_code_key")));

        webTestClient.post().uri("/api/v1/email-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"code":"DUP","name":"Dup","subject":"S","body":"B"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Data conflict");
    }

    @Test
    void deleteReturns204WithoutBody() {
        given(service.delete(3L)).willReturn(Mono.empty());

        webTestClient.delete().uri("/api/v1/email-templates/3")
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void unexpectedErrorReturnsOpaque500() {
        given(service.findById(1L)).willReturn(Mono.error(new IllegalStateException("secret internal detail")));

        webTestClient.get().uri("/api/v1/email-templates/1")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Internal server error")
                // Internals must not leak to API clients.
                .jsonPath("$.detail").isEqualTo("An unexpected error occurred.");
    }
}
