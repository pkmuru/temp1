package com.ubs.wma.aat.rampuppack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ubs.wma.aat.rampuppack.client.staatemail.StaatEmailClient;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.StaatSendResponse;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.UploadAttachmentRequest;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.UploadAttachmentResponse;
import com.ubs.wma.aat.rampuppack.config.properties.EmailProperties;
import com.ubs.wma.aat.rampuppack.config.properties.SchedulerProperties;
import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailLog;
import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import com.ubs.wma.aat.rampuppack.dto.EmailBatchRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailBatchResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailPreviewResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailSendRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailSendResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailTemplateRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailTemplateResponse;
import com.ubs.wma.aat.rampuppack.repository.EmailBatchRepository;
import com.ubs.wma.aat.rampuppack.repository.EmailLogRepository;
import com.ubs.wma.aat.rampuppack.service.EmailDeliveryScheduler;
import com.ubs.wma.aat.rampuppack.service.EmailSendService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

/**
 * End-to-end delivery flows through the real stack (WebFlux → services → R2DBC → embedded
 * PostgreSQL) with only the outbound {@link StaatEmailClient} mocked. Insight documents come
 * from {@code db/seed.sql} (ACE-1001..ACE-1003), exactly as datamesh would provide them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class EmailDeliveryFlowTest extends AbstractIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    EmailLogRepository emailLogRepository;

    @Autowired
    EmailBatchRepository emailBatchRepository;

    @Autowired
    EmailSendService emailSendService;

    @Autowired
    EmailProperties emailProperties;

    @MockitoBean
    StaatEmailClient staatEmailClient;

    @Test
    void sendDeliversAndStoresMergedContent() {
        stubStaatEmailHealthy();
        Long templateId = createTemplate("E2E_SEND_OK");
        Long failTemplateId = createTemplate("E2E_SEND_OK_FAIL");

        EmailSendResponse response = postSend(
                        sendRequest(templateId, failTemplateId, "FA-OK", List.of("ACE-1001", "ACE-1002")))
                .expectStatus()
                .isOk()
                .expectBody(EmailSendResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.parts()).hasSize(1);
        var part = response.parts().get(0);
        assertThat(part.status()).isEqualTo(EmailStatus.SENT);
        assertThat(part.smeId()).isNotBlank();
        assertThat(part.attachmentFileNames()).containsExactly("John_Smith_Household.html", "Jane_Doe_Household.html");

        EmailLog logged = emailLogRepository.findById(part.emailLogId()).block();
        assertThat(logged.mergedSubject()).isEqualTo("Packs for Alex Advisor");
        assertThat(logged.mergedBody())
                .contains("Hi Alex Advisor, 2 pack(s).")
                .contains("John Smith Household")
                .contains("Jane Doe Household");
        assertThat(logged.attemptCount()).isEqualTo(1);
    }

    @Test
    void failedSendRecordsFailureAndTriggersOneNotice() {
        stubStaatEmailDown("STAAT email service unavailable");
        Long templateId = createTemplate("E2E_SEND_KO");
        Long failTemplateId = createTemplate("E2E_SEND_KO_FAIL");

        EmailSendResponse response = postSend(sendRequest(templateId, failTemplateId, "FA-KO", List.of("ACE-1001")))
                .expectStatus()
                .isOk()
                .expectBody(EmailSendResponse.class)
                .returnResult()
                .getResponseBody();

        var part = response.parts().get(0);
        assertThat(part.status()).isEqualTo(EmailStatus.FAILED);
        assertThat(part.failureReason()).contains("unavailable");

        // The failure notice is its own log row, rendered from the fail template, no attachments,
        // flagged so its own failure can never trigger another notice.
        List<EmailLog> notices = emailLogRepository
                .findAll()
                .filter(row -> row.failureNotification() && "FA-KO".equals(row.faId()))
                .collectList()
                .block();
        assertThat(notices).hasSize(1);
        assertThat(notices.get(0).templateId()).isEqualTo(failTemplateId);
        assertThat(notices.get(0).failTemplateId()).isNull();
        assertThat(notices.get(0).attachmentFileNames()).isEmpty();
    }

    @Test
    void previewMergesLikeSendButDeliversNothing() {
        Long templateId = createTemplate("E2E_PREVIEW");
        Long failTemplateId = createTemplate("E2E_PREVIEW_FAIL");

        EmailPreviewResponse preview = webTestClient
                .post()
                .uri("/api/v1/emails/preview")
                .bodyValue(sendRequest(templateId, failTemplateId, "FA-PREVIEW", List.of("ACE-1001", "ACE-1003")))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(EmailPreviewResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(preview.recipientEmail()).isEqualTo("fa@ubs.com");
        assertThat(preview.parts()).hasSize(1);
        var part = preview.parts().get(0);
        assertThat(part.subject()).isEqualTo("Packs for Alex Advisor");
        assertThat(part.htmlContent())
                .contains("Hi Alex Advisor, 2 pack(s).")
                .contains("John Smith Household")
                .contains("Brown Family");

        verifyNoInteractions(staatEmailClient);
        Long loggedRows = emailLogRepository
                .findAll()
                .filter(row -> templateId.equals(row.templateId()))
                .count()
                .block();
        assertThat(loggedRows).isZero();
    }

    @Test
    void missingInsightDocumentsRejectTheWholeSend() {
        Long templateId = createTemplate("E2E_MISSING");
        Long failTemplateId = createTemplate("E2E_MISSING_FAIL");

        postSend(sendRequest(templateId, failTemplateId, "FA-MISSING", List.of("ACE-1001", "ACE-NOPE")))
                .expectStatus()
                .isEqualTo(422)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Missing insight documents")
                .jsonPath("$.missingAceIds[0]")
                .isEqualTo("ACE-NOPE");

        verifyNoInteractions(staatEmailClient);
    }

    @Test
    void dueBatchIsClaimedProcessedAndCompletedByTheScheduler() {
        stubStaatEmailHealthy();
        Long templateId = createTemplate("E2E_BATCH");
        Long failTemplateId = createTemplate("E2E_BATCH_FAIL");

        EmailBatchResponse created = webTestClient
                .post()
                .uri("/api/v1/email-batches")
                .bodyValue(new EmailBatchRequest(
                        List.of("ACE-1003"),
                        null,
                        "FL-7",
                        templateId,
                        failTemplateId,
                        "fl7@ubs.com",
                        Map.of("faName", "Pat Leader"),
                        Instant.now().minusSeconds(60)))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(EmailBatchResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(created.status()).isEqualTo(BatchStatus.SCHEDULED);

        scheduler().processDueBatches();

        webTestClient
                .get()
                .uri("/api/v1/email-batches/{id}", created.id())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(EmailBatchResponse.class)
                .value(batch -> {
                    assertThat(batch.status()).isEqualTo(BatchStatus.COMPLETED);
                    assertThat(batch.processedAt()).isNotNull();
                });

        List<EmailLog> batchLogs =
                emailLogRepository.findByBatchId(created.id()).collectList().block();
        assertThat(batchLogs).hasSize(1);
        assertThat(batchLogs.get(0).status()).isEqualTo(EmailStatus.SENT);
    }

    @Test
    void failedDeliveryIsRetriedByTheSchedulerUntilItSucceeds() {
        stubStaatEmailDown("temporary outage");
        Long templateId = createTemplate("E2E_RETRY");
        Long failTemplateId = createTemplate("E2E_RETRY_FAIL");

        EmailSendResponse response = postSend(sendRequest(templateId, failTemplateId, "FA-RETRY", List.of("ACE-1002")))
                .expectStatus()
                .isOk()
                .expectBody(EmailSendResponse.class)
                .returnResult()
                .getResponseBody();
        Long failedLogId = response.parts().get(0).emailLogId();
        assertThat(response.parts().get(0).status()).isEqualTo(EmailStatus.FAILED);

        stubStaatEmailHealthy(); // STAAT recovers before the next scheduler tick
        scheduler().retryFailedDeliveries();

        EmailLog retried = emailLogRepository.findById(failedLogId).block();
        assertThat(retried.status()).isEqualTo(EmailStatus.SENT);
        assertThat(retried.attemptCount()).isGreaterThanOrEqualTo(2);
        assertThat(retried.failureReason()).isNull();
    }

    // ------------------------------------------------------------- helpers

    /** Same wiring as the production bean (disabled in the test profile) — passes run on demand. */
    private EmailDeliveryScheduler scheduler() {
        return new EmailDeliveryScheduler(
                emailBatchRepository,
                emailLogRepository,
                emailSendService,
                new SchedulerProperties(true, Duration.ofHours(1), 25),
                emailProperties);
    }

    // Stubbing uses the will*().given() form so a test can re-stub (outage → recovery) without
    // Mockito invoking the previous answer mid-stubbing.

    private void stubStaatEmailHealthy() {
        stubUploadOk();
        willReturn(Mono.just(new StaatSendResponse("sme-ok", "ACCEPTED")))
                .given(staatEmailClient)
                .sendEmail(any());
    }

    private void stubStaatEmailDown(String reason) {
        stubUploadOk();
        willReturn(Mono.error(new IllegalStateException(reason)))
                .given(staatEmailClient)
                .sendEmail(any());
    }

    private void stubUploadOk() {
        willAnswer(invocation -> {
                    UploadAttachmentRequest request = invocation.getArgument(0);
                    return Mono.just(new UploadAttachmentResponse(request.fileName(), "ref-" + request.fileName()));
                })
                .given(staatEmailClient)
                .uploadAttachment(any());
    }

    private Long createTemplate(String code) {
        EmailTemplateRequest request = new EmailTemplateRequest(
                code,
                code,
                null,
                "Packs for {faName}",
                "<p>Hi {faName}, {packCount} pack(s).</p>{householdTable}",
                null);
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
        return response.id();
    }

    private static EmailSendRequest sendRequest(
            Long templateId, Long failTemplateId, String faId, List<String> aceIds) {
        return new EmailSendRequest(
                aceIds, faId, null, templateId, failTemplateId, "fa@ubs.com", Map.of("faName", "Alex Advisor"));
    }

    private WebTestClient.ResponseSpec postSend(EmailSendRequest request) {
        return webTestClient
                .post()
                .uri("/api/v1/emails/send")
                .bodyValue(request)
                .exchange();
    }
}
