package com.ubs.wma.aat.rampuppack.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ubs.wma.aat.rampuppack.TestData;
import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailBatch;
import com.ubs.wma.aat.rampuppack.exception.ConflictException;
import com.ubs.wma.aat.rampuppack.exception.ResourceNotFoundException;
import com.ubs.wma.aat.rampuppack.service.EmailBatchService;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Web-layer slice test for {@link EmailBatchController}: 201 on enqueue, validation of the
 * batch payload (send fields + {@code scheduledAt}), status-filter binding and the 409 mapping
 * when cancelling an already-processed entry.
 */
@WebFluxTest(EmailBatchController.class)
class EmailBatchControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    EmailBatchService service;

    private static final String VALID_BODY =
            """
            {"aceIds":["ACE-1003"],"fieldLeaderId":"FL-7","templateId":2,"failTemplateId":3,
             "recipientEmail":"fl7@ubs.com","mergeFields":{"fieldLeaderName":"Pat Leader"},
             "scheduledAt":"2026-07-01T08:00:00Z"}
            """;

    @Test
    void addReturns201WithTheScheduledEntry() {
        given(service.schedule(any())).willReturn(Mono.just(TestData.batch(BatchStatus.SCHEDULED)));

        webTestClient
                .post()
                .uri("/api/v1/email-batches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(VALID_BODY)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(11)
                .jsonPath("$.status")
                .isEqualTo("SCHEDULED")
                .jsonPath("$.scheduledAt")
                .isEqualTo("2026-07-01T08:00:00Z")
                .jsonPath("$.mergeFields.fieldLeaderName")
                .isEqualTo("Pat Leader");

        // The mapped entity must carry the request values into the service.
        ArgumentCaptor<EmailBatch> entity = ArgumentCaptor.forClass(EmailBatch.class);
        then(service).should().schedule(entity.capture());
        assertThat(entity.getValue().fieldLeaderId()).isEqualTo("FL-7");
        assertThat(entity.getValue().scheduledAt()).isEqualTo(TestData.SCHEDULED_AT);
        assertThat(entity.getValue().status()).isEqualTo(BatchStatus.SCHEDULED);
    }

    static Stream<Arguments> invalidBatchBodies() {
        return Stream.of(
                arguments(
                        "missing scheduledAt",
                        """
                        {"aceIds":["ACE-1003"],"fieldLeaderId":"FL-7","templateId":2,
                         "failTemplateId":3,"recipientEmail":"fl7@ubs.com"}
                        """,
                        "scheduledAt"),
                arguments(
                        "both faId and fieldLeaderId set",
                        """
                        {"aceIds":["ACE-1003"],"faId":"FA-42","fieldLeaderId":"FL-7","templateId":2,
                         "failTemplateId":3,"recipientEmail":"fl7@ubs.com",
                         "scheduledAt":"2026-07-01T08:00:00Z"}
                        """,
                        "exactly one of faId or fieldLeaderId"),
                arguments(
                        "malformed recipient email",
                        """
                        {"aceIds":["ACE-1003"],"fieldLeaderId":"FL-7","templateId":2,
                         "failTemplateId":3,"recipientEmail":"not-an-email",
                         "scheduledAt":"2026-07-01T08:00:00Z"}
                        """,
                        "recipientEmail"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBatchBodies")
    void invalidBatchRequestsAreRejectedWithoutReachingTheService(
            String description, String body, String expectedInDetail) {
        webTestClient
                .post()
                .uri("/api/v1/email-batches")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Validation failed")
                .jsonPath("$.detail")
                .value(detail -> assertThat(detail.toString()).contains(expectedInDetail));

        then(service).shouldHaveNoInteractions();
    }

    @Test
    void listBindsTheStatusFilter() {
        given(service.findAll(BatchStatus.SCHEDULED)).willReturn(Flux.just(TestData.batch(BatchStatus.SCHEDULED)));

        webTestClient
                .get()
                .uri("/api/v1/email-batches?status=SCHEDULED")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo(11);

        then(service).should().findAll(BatchStatus.SCHEDULED);
    }

    @Test
    void getUnknownIdReturns404ProblemDetail() {
        given(service.findById(99L)).willReturn(Mono.error(new ResourceNotFoundException("Email batch", 99L)));

        webTestClient
                .get()
                .uri("/api/v1/email-batches/99")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Resource not found");
    }

    @Test
    void cancelReturnsTheCancelledEntry() {
        given(service.cancel(11L)).willReturn(Mono.just(TestData.batch(BatchStatus.CANCELLED)));

        webTestClient
                .delete()
                .uri("/api/v1/email-batches/11")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("CANCELLED");
    }

    @Test
    void cancellingAProcessedEntryMapsTo409() {
        given(service.cancel(11L))
                .willReturn(Mono.error(
                        new ConflictException("Email batch 11 is COMPLETED and can no longer be cancelled")));

        webTestClient
                .delete()
                .uri("/api/v1/email-batches/11")
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Conflict")
                .jsonPath("$.detail")
                .value(detail -> assertThat(detail.toString()).contains("can no longer be cancelled"));
    }
}
