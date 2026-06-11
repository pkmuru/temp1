package com.ubs.wma.aat.rampuppack.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ubs.wma.aat.rampuppack.TestData;
import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import com.ubs.wma.aat.rampuppack.dto.EmailPreviewPart;
import com.ubs.wma.aat.rampuppack.dto.EmailPreviewResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailSendRequest;
import com.ubs.wma.aat.rampuppack.exception.MissingInsightDocumentException;
import com.ubs.wma.aat.rampuppack.service.EmailSendService;
import java.util.List;
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
 * Web-layer slice test for {@link EmailController}: request validation (including the
 * exactly-one-of faId/fieldLeaderId rule), enum query-parameter binding, JSON shape and the
 * 422 missing-documents mapping. The send pipeline itself is mocked — it is covered end-to-end
 * by {@code EmailDeliveryFlowTest}.
 */
@WebFluxTest(EmailController.class)
class EmailControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    EmailSendService service;

    /** A valid send body with the recipient-identifier JSON fragment swapped per test case. */
    private static String sendBody(String identifiers) {
        return """
                {"aceIds":["ACE-1001"],%s,"templateId":1,"failTemplateId":2,
                 "recipientEmail":"fa42@ubs.com","mergeFields":{"faName":"Alex Advisor"}}
                """
                .formatted(identifiers);
    }

    @Test
    void sendReturnsPerPartResults() {
        given(service.send(any(), isNull())).willReturn(Mono.just(List.of(TestData.sentLog())));

        webTestClient
                .post()
                .uri("/api/v1/emails/send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sendBody("\"faId\":\"FA-42\""))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.parts[0].emailLogId")
                .isEqualTo(42)
                .jsonPath("$.parts[0].partNumber")
                .isEqualTo(1)
                .jsonPath("$.parts[0].totalParts")
                .isEqualTo(1)
                .jsonPath("$.parts[0].status")
                .isEqualTo("SENT")
                .jsonPath("$.parts[0].smeId")
                .isEqualTo("sme-abc")
                .jsonPath("$.parts[0].attachmentFileNames[0]")
                .isEqualTo("John_Smith_Household.html");

        // The controller passes the bound request through unchanged, with batchId=null (live send).
        ArgumentCaptor<EmailSendRequest> request = ArgumentCaptor.forClass(EmailSendRequest.class);
        then(service).should().send(request.capture(), isNull());
        assertThat(request.getValue().faId()).isEqualTo("FA-42");
        assertThat(request.getValue().mergeFields()).containsEntry("faName", "Alex Advisor");
    }

    static Stream<Arguments> invalidSendBodies() {
        return Stream.of(
                arguments(
                        "both faId and fieldLeaderId set",
                        sendBody("\"faId\":\"FA-42\",\"fieldLeaderId\":\"FL-7\""),
                        "exactly one of faId or fieldLeaderId"),
                arguments(
                        "neither faId nor fieldLeaderId set",
                        sendBody("\"faId\":null"),
                        "exactly one of faId or fieldLeaderId"),
                arguments(
                        "blank entry inside aceIds",
                        """
                        {"aceIds":[""],"faId":"FA-42","templateId":1,"failTemplateId":2,
                         "recipientEmail":"fa42@ubs.com"}
                        """,
                        "aceIds"),
                arguments(
                        "empty aceIds list",
                        """
                        {"aceIds":[],"faId":"FA-42","templateId":1,"failTemplateId":2,
                         "recipientEmail":"fa42@ubs.com"}
                        """,
                        "aceIds"),
                arguments(
                        "malformed recipient email",
                        """
                        {"aceIds":["ACE-1001"],"faId":"FA-42","templateId":1,"failTemplateId":2,
                         "recipientEmail":"not-an-email"}
                        """,
                        "recipientEmail"),
                arguments(
                        "missing templateId",
                        """
                        {"aceIds":["ACE-1001"],"faId":"FA-42","failTemplateId":2,
                         "recipientEmail":"fa42@ubs.com"}
                        """,
                        "templateId"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSendBodies")
    void invalidSendRequestsAreRejectedWithoutReachingTheService(
            String description, String body, String expectedInDetail) {
        webTestClient
                .post()
                .uri("/api/v1/emails/send")
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
    void sendWithMalformedJsonReturns400() {
        webTestClient
                .post()
                .uri("/api/v1/emails/send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{not json")
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Bad request");
    }

    @Test
    void missingInsightDocumentsMapTo422WithTheMissingIds() {
        given(service.send(any(), isNull()))
                .willReturn(Mono.error(new MissingInsightDocumentException(List.of("ACE-NOPE"))));

        webTestClient
                .post()
                .uri("/api/v1/emails/send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sendBody("\"faId\":\"FA-42\""))
                .exchange()
                .expectStatus()
                .isEqualTo(422)
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Missing insight documents")
                .jsonPath("$.missingAceIds[0]")
                .isEqualTo("ACE-NOPE");
    }

    @Test
    void previewReturnsMergedHtmlWithoutPartResults() {
        given(service.preview(any()))
                .willReturn(Mono.just(new EmailPreviewResponse(
                        "fa42@ubs.com",
                        List.of(new EmailPreviewPart(
                                1,
                                2,
                                "Subject (Part 1 of 2)",
                                "<p>Hi Alex</p>",
                                List.of("ACE-1001"),
                                List.of("John_Smith_Household.html"))))));

        webTestClient
                .post()
                .uri("/api/v1/emails/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sendBody("\"faId\":\"FA-42\""))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.recipientEmail")
                .isEqualTo("fa42@ubs.com")
                .jsonPath("$.parts[0].subject")
                .isEqualTo("Subject (Part 1 of 2)")
                .jsonPath("$.parts[0].htmlContent")
                .isEqualTo("<p>Hi Alex</p>")
                .jsonPath("$.parts[0].totalParts")
                .isEqualTo(2);
    }

    @Test
    void previewValidatesLikeSend() {
        webTestClient
                .post()
                .uri("/api/v1/emails/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sendBody("\"faId\":\"FA-42\",\"fieldLeaderId\":\"FL-7\""))
                .exchange()
                .expectStatus()
                .isBadRequest();

        then(service).shouldHaveNoInteractions();
    }

    @Test
    void listBindsStatusAndBatchIdFilters() {
        given(service.findLogs(EmailStatus.FAILED, 7L)).willReturn(Flux.just(TestData.sentLog()));

        webTestClient
                .get()
                .uri("/api/v1/emails?status=FAILED&batchId=7")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo(42);

        then(service).should().findLogs(EmailStatus.FAILED, 7L);
    }

    @Test
    void listWithUnknownStatusValueReturns400() {
        webTestClient
                .get()
                .uri("/api/v1/emails?status=NOT_A_STATUS")
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Bad request");

        then(service).shouldHaveNoInteractions();
    }

    @Test
    void getReturnsTheStoredMergedContent() {
        given(service.findById(42L)).willReturn(Mono.just(TestData.sentLog()));

        webTestClient
                .get()
                .uri("/api/v1/emails/42")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.mergedSubject")
                .isEqualTo("STAAT Client Retention Packs")
                .jsonPath("$.mergedBody")
                .isEqualTo("<p>Hi Alex Advisor</p>")
                .jsonPath("$.mergeFields.faName")
                .isEqualTo("Alex Advisor")
                .jsonPath("$.status")
                .isEqualTo("SENT")
                .jsonPath("$.failureNotification")
                .isEqualTo(false);
    }
}
