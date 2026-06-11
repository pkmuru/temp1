package com.ubs.wma.aat.rampuppack.service;

import com.ubs.wma.aat.rampuppack.client.staatemail.StaatEmailClient;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.AttachmentRef;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.MailRecipient;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.MailRequestDetails;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.StaatSendRequest;
import com.ubs.wma.aat.rampuppack.client.staatemail.dto.UploadAttachmentRequest;
import com.ubs.wma.aat.rampuppack.config.properties.EmailProperties;
import com.ubs.wma.aat.rampuppack.config.properties.StaatEmailProperties;
import com.ubs.wma.aat.rampuppack.domain.EmailLog;
import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailTemplate;
import com.ubs.wma.aat.rampuppack.domain.StaatInsightDocument;
import com.ubs.wma.aat.rampuppack.dto.EmailPreviewResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailSendRequest;
import com.ubs.wma.aat.rampuppack.exception.ResourceNotFoundException;
import com.ubs.wma.aat.rampuppack.mapper.EmailMapper;
import com.ubs.wma.aat.rampuppack.repository.EmailLogRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The send pipeline, shared by the live API, the preview API, batch processing and scheduler
 * retries. The flow is split into a small number of named steps:
 *
 * <ol>
 *   <li>{@link #loadContext} — resolve both templates and fetch all insight documents (I/O);</li>
 *   <li>{@link #planParts} — pure Java, no I/O: split the documents into size-capped parts and
 *       render each part's merged subject/body into a pending {@code email_log} row.
 *       {@link #preview} stops here and returns the plan to the UI;</li>
 *   <li>{@link #deliverPart} — save the row, then {@link #attempt} delivery: upload attachments,
 *       call StaatEmail, persist the outcome ({@code SENT}/{@code FAILED}).</li>
 * </ol>
 *
 * <p>A part that fails delivery stays {@code FAILED} (the scheduler retries it for up to the
 * configured window) and, on its <em>first</em> failure only, triggers a best-effort
 * failure-notification email rendered from the request's fail template ("we are retrying,
 * no action needed" — per requirement).
 */
@Service
public class EmailSendService {

    private static final Logger log = LoggerFactory.getLogger(EmailSendService.class);
    private static final int FAILURE_REASON_MAX_LENGTH = 2000;

    private final EmailTemplateService templateService;
    private final InsightDocumentService insightDocumentService;
    private final EmailLogRepository emailLogRepository;
    private final StaatEmailClient staatEmailClient;
    private final StaatEmailProperties staatEmailProperties;
    private final EmailProperties emailProperties;

    public EmailSendService(
            EmailTemplateService templateService,
            InsightDocumentService insightDocumentService,
            EmailLogRepository emailLogRepository,
            StaatEmailClient staatEmailClient,
            StaatEmailProperties staatEmailProperties,
            EmailProperties emailProperties) {
        this.templateService = templateService;
        this.insightDocumentService = insightDocumentService;
        this.emailLogRepository = emailLogRepository;
        this.staatEmailClient = staatEmailClient;
        this.staatEmailProperties = staatEmailProperties;
        this.emailProperties = emailProperties;
    }

    // ------------------------------------------------------------------ API

    /**
     * Runs the full pipeline for one send request; {@code batchId} links the resulting log rows
     * to their batch entry ({@code null} for live sends). Emits one {@link EmailLog} per part,
     * in part order.
     */
    public Mono<List<EmailLog>> send(EmailSendRequest request, Long batchId) {
        return loadContext(request).flatMap(context -> {
            List<PlannedPart> parts = planParts(request, batchId, context);
            // Sequential per requirement: parts are delivered one after the other, in order.
            return Flux.fromIterable(parts)
                    .concatMap(part -> deliverPart(part, context.failTemplate()))
                    .collectList();
        });
    }

    /**
     * Same pipeline as {@link #send} — same payload, same validation, same splitting and merging —
     * but the merged result goes back to the UI instead of to StaatEmail. Nothing is logged.
     */
    public Mono<EmailPreviewResponse> preview(EmailSendRequest request) {
        return loadContext(request).map(context -> {
            List<EmailLog> plannedRows = planParts(request, null, context).stream()
                    .map(PlannedPart::pendingRow)
                    .toList();
            return EmailMapper.toPreviewResponse(request.recipientEmail(), plannedRows);
        });
    }

    /**
     * One retry of a previously failed (re-claimed) log row. The stored merged subject/body are
     * reused verbatim; attachments are re-fetched by the row's {@code aceIds} and re-uploaded.
     * Never errors — a failed retry is persisted as {@code FAILED} again and stays eligible
     * until the retry window elapses.
     */
    public Mono<EmailLog> retry(EmailLog claimed) {
        Mono<List<StaatInsightDocument>> attachments = claimed.failureNotification()
                ? Mono.just(List.of())
                : insightDocumentService.requireAll(claimed.aceIds());
        return attachments
                .flatMap(documents -> attempt(claimed, documents))
                .onErrorResume(e -> emailLogRepository.save(claimed.asFailed(null, failureReason(e), Instant.now())));
    }

    public Mono<EmailLog> findById(Long id) {
        return emailLogRepository
                .findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Email log", id)));
    }

    public Flux<EmailLog> findLogs(EmailStatus status, Long batchId) {
        if (batchId != null) {
            return emailLogRepository.findByBatchId(batchId).filter(row -> status == null || row.status() == status);
        }
        return status != null ? emailLogRepository.findByStatus(status) : emailLogRepository.findAll();
    }

    // ------------------------------------------------- step 1: load context

    /** Everything the pipeline needs from the database: both templates and all insight documents. */
    private record SendContext(
            EmailTemplate template, EmailTemplate failTemplate, List<StaatInsightDocument> documents) {}

    private Mono<SendContext> loadContext(EmailSendRequest request) {
        return Mono.zip(
                        templateService.requireActive(request.templateId()),
                        templateService.requireActive(request.failTemplateId()),
                        insightDocumentService.requireAll(request.aceIds()))
                .map(loaded -> new SendContext(loaded.getT1(), loaded.getT2(), loaded.getT3()));
    }

    // ---------------------------------------------- step 2: plan (pure Java)

    /** A pending log row with its merged content, plus the documents to attach to this part. */
    private record PlannedPart(EmailLog pendingRow, List<StaatInsightDocument> documents) {}

    /** Pure planning, no I/O: split the documents into parts and render each part's row. */
    private List<PlannedPart> planParts(EmailSendRequest request, Long batchId, SendContext context) {
        List<List<StaatInsightDocument>> split = splitBySize(context.documents(), emailProperties.maxAttachmentBytes());
        List<PlannedPart> parts = new ArrayList<>(split.size());
        for (int index = 0; index < split.size(); index++) {
            parts.add(planPart(request, batchId, context.template(), split.get(index), index + 1, split.size()));
        }
        return parts;
    }

    private PlannedPart planPart(
            EmailSendRequest request,
            Long batchId,
            EmailTemplate template,
            List<StaatInsightDocument> documents,
            int partNumber,
            int totalParts) {
        Map<String, String> fields =
                effectiveMergeFields(request.mergeFields(), documents, partNumber, totalParts, null);
        String subject = TemplateMerger.merge(template.subject(), fields) + partSuffix(partNumber, totalParts);
        String body = TemplateMerger.merge(template.body(), fields);
        List<String> aceIds =
                documents.stream().map(StaatInsightDocument::aceId).toList();
        List<String> fileNames =
                documents.stream().map(StaatInsightDocument::fileName).toList();

        EmailLog pendingRow = EmailLog.pendingPart(
                batchId,
                template.id(),
                request.failTemplateId(),
                request.faId(),
                request.fieldLeaderId(),
                request.recipientEmail(),
                aceIds,
                fields,
                subject,
                body,
                fileNames,
                partNumber,
                totalParts);
        return new PlannedPart(pendingRow, documents);
    }

    /**
     * Greedy split keeping each part's total attachment size within {@code maxBytes}; a single
     * oversized document still ships alone (a document cannot be split). Order is preserved.
     */
    static List<List<StaatInsightDocument>> splitBySize(List<StaatInsightDocument> documents, long maxBytes) {
        List<List<StaatInsightDocument>> parts = new ArrayList<>();
        List<StaatInsightDocument> current = new ArrayList<>();
        long currentBytes = 0;
        for (StaatInsightDocument document : documents) {
            long size = document.contentSizeBytes();
            if (!current.isEmpty() && currentBytes + size > maxBytes) {
                parts.add(current);
                current = new ArrayList<>();
                currentBytes = 0;
            }
            current.add(document);
            currentBytes += size;
        }
        if (!current.isEmpty()) {
            parts.add(current);
        }
        return parts;
    }

    private static String partSuffix(int partNumber, int totalParts) {
        return totalParts > 1 ? " (Part %d of %d)".formatted(partNumber, totalParts) : "";
    }

    // -------------------------------------------------- step 3: deliver

    private Mono<EmailLog> deliverPart(PlannedPart part, EmailTemplate failTemplate) {
        return emailLogRepository
                .save(part.pendingRow())
                .flatMap(saved -> attempt(saved, part.documents()))
                .flatMap(outcome -> isFirstFailure(outcome)
                        ? notifyDeliveryFailure(outcome, failTemplate, part.documents())
                                .thenReturn(outcome)
                        : Mono.just(outcome));
    }

    private static boolean isFirstFailure(EmailLog outcome) {
        return outcome.status() == EmailStatus.FAILED && outcome.attemptCount() == 1;
    }

    /**
     * One delivery attempt for a saved log row: upload every attachment, send the email, persist
     * the outcome ({@code SENT} or {@code FAILED} with the reason). Never errors.
     */
    private Mono<EmailLog> attempt(EmailLog emailLog, List<StaatInsightDocument> attachments) {
        String smeId = UUID.randomUUID().toString();
        return uploadAttachments(attachments)
                .flatMap(refs -> staatEmailClient.sendEmail(buildSendRequest(smeId, emailLog, refs)))
                .map(response -> emailLog.asSent(smeId, Instant.now()))
                .onErrorResume(e -> {
                    log.warn(
                            "Delivery attempt {} failed for email_log {} (recipient {}): {}",
                            emailLog.attemptCount() + 1,
                            emailLog.id(),
                            emailLog.recipientEmail(),
                            e.toString());
                    return Mono.just(emailLog.asFailed(smeId, failureReason(e), Instant.now()));
                })
                .flatMap(emailLogRepository::save);
    }

    /** Uploads sequentially and collects the attachment references for the send call. */
    private Mono<List<AttachmentRef>> uploadAttachments(List<StaatInsightDocument> documents) {
        return Flux.fromIterable(documents).concatMap(this::uploadAttachment).collectList();
    }

    private Mono<AttachmentRef> uploadAttachment(StaatInsightDocument document) {
        String contentBase64 =
                Base64.getEncoder().encodeToString(document.htmlContent().getBytes(StandardCharsets.UTF_8));
        return staatEmailClient
                .uploadAttachment(new UploadAttachmentRequest(document.fileName(), contentBase64))
                .map(uploaded -> new AttachmentRef(uploaded.attachmentReferenceId(), uploaded.fileName()));
    }

    private StaatSendRequest buildSendRequest(String smeId, EmailLog emailLog, List<AttachmentRef> attachments) {
        var mailDetails = new MailRequestDetails(
                emailLog.mergedSubject(), staatEmailProperties.replyTo(), emailLog.mergedBody(), attachments);
        return new StaatSendRequest(
                smeId,
                smeId,
                staatEmailProperties.senderGpn(),
                staatEmailProperties.fromGpn(),
                staatEmailProperties.fromAddress(),
                staatEmailProperties.senderAddress(),
                List.of(MailRecipient.to(emailLog.recipientEmail())),
                staatEmailProperties.applicationName(),
                mailDetails);
    }

    // --------------------------------------------- failure notification

    /**
     * Best effort: render the fail template and send/log a failure-notification email for a part
     * that just failed for the first time. The notice row never notifies again on its own failure
     * ({@code failureNotification=true}) and never breaks the original send's result.
     */
    private Mono<Void> notifyDeliveryFailure(
            EmailLog failedPart, EmailTemplate failTemplate, List<StaatInsightDocument> partDocuments) {
        Map<String, String> fields = effectiveMergeFields(
                failedPart.mergeFields(),
                partDocuments,
                failedPart.partNumber(),
                failedPart.totalParts(),
                failedPart.failureReason());
        String subject = TemplateMerger.merge(failTemplate.subject(), fields);
        String body = TemplateMerger.merge(failTemplate.body(), fields);

        return emailLogRepository
                .save(EmailLog.failureNotice(failedPart, fields, subject, body))
                .flatMap(notice -> attempt(notice, List.of()))
                .doOnNext(notice -> log.info(
                        "Failure notification {} for email_log {} is {}",
                        notice.id(),
                        failedPart.id(),
                        notice.status()))
                .onErrorResume(e -> {
                    log.error("Could not record failure notification for email_log {}", failedPart.id(), e);
                    return Mono.empty();
                })
                .then();
    }

    // ----------------------------------------------------------- helpers

    /**
     * Caller-supplied merge fields plus the system-provided ones ({@code packCount},
     * {@code householdTable}, {@code partNumber}, {@code totalParts} and — for failure
     * notices — {@code failureReason}); system fields win on a name clash.
     */
    private static Map<String, String> effectiveMergeFields(
            Map<String, String> callerFields,
            List<StaatInsightDocument> documents,
            int partNumber,
            int totalParts,
            String failureReason) {
        Map<String, String> fields = new LinkedHashMap<>(callerFields);
        fields.put("packCount", String.valueOf(documents.size()));
        fields.put("householdTable", householdTable(documents));
        fields.put("partNumber", String.valueOf(partNumber));
        fields.put("totalParts", String.valueOf(totalParts));
        if (failureReason != null) {
            fields.put("failureReason", failureReason);
        }
        return fields;
    }

    /** HTML table of lead client name / file name, one row per retention pack in the part. */
    private static String householdTable(List<StaatInsightDocument> documents) {
        if (documents.isEmpty()) {
            return "";
        }
        StringBuilder table = new StringBuilder("<table><tr><th>Household Name</th><th>File Name</th></tr>");
        for (StaatInsightDocument document : documents) {
            table.append("<tr><td>")
                    .append(document.leadClientName())
                    .append("</td><td>")
                    .append(document.fileName())
                    .append("</td></tr>");
        }
        return table.append("</table>").toString();
    }

    /** Failure reason fitted to its column, shared with the scheduler. */
    static String failureReason(Throwable e) {
        String reason = e.getMessage() != null ? e.getMessage() : e.toString();
        return reason.length() > FAILURE_REASON_MAX_LENGTH ? reason.substring(0, FAILURE_REASON_MAX_LENGTH) : reason;
    }
}
