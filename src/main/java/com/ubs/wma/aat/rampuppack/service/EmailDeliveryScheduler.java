package com.ubs.wma.aat.rampuppack.service;

import com.ubs.wma.aat.rampuppack.config.properties.EmailProperties;
import com.ubs.wma.aat.rampuppack.config.properties.SchedulerProperties;
import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailBatch;
import com.ubs.wma.aat.rampuppack.domain.EmailLog;
import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import com.ubs.wma.aat.rampuppack.mapper.EmailMapper;
import com.ubs.wma.aat.rampuppack.repository.EmailBatchRepository;
import com.ubs.wma.aat.rampuppack.repository.EmailLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Periodic poller (interval: {@code app.scheduler.poll-interval}, ISO-8601 — hourly, daily, ...)
 * with three passes per tick:
 * <ol>
 *   <li>claim due {@code email_batch} rows and run them through the send pipeline;</li>
 *   <li>claim {@code FAILED} log rows still inside the retry window and retry them;</li>
 *   <li>mark {@code FAILED} rows whose window elapsed as {@code EXHAUSTED} and log them
 *       for follow-up.</li>
 * </ol>
 * All claims are atomic {@code UPDATE … RETURNING} statements with {@code SKIP LOCKED}, so
 * multiple service instances can poll concurrently without double-sending. Each pass blocks the
 * scheduler thread (never an event-loop thread); {@code fixedDelay} prevents overlapping ticks.
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class EmailDeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryScheduler.class);
    private static final Duration PASS_TIMEOUT = Duration.ofMinutes(10);

    private final EmailBatchRepository batchRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailSendService emailSendService;
    private final SchedulerProperties schedulerProperties;
    private final EmailProperties emailProperties;

    public EmailDeliveryScheduler(
            EmailBatchRepository batchRepository,
            EmailLogRepository emailLogRepository,
            EmailSendService emailSendService,
            SchedulerProperties schedulerProperties,
            EmailProperties emailProperties) {
        this.batchRepository = batchRepository;
        this.emailLogRepository = emailLogRepository;
        this.emailSendService = emailSendService;
        this.schedulerProperties = schedulerProperties;
        this.emailProperties = emailProperties;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.poll-interval:PT1H}")
    public void poll() {
        processDueBatches();
        retryFailedDeliveries();
        exhaustExpiredRetries();
    }

    public void processDueBatches() {
        try {
            Long processed = batchRepository
                    .claimDue(schedulerProperties.claimLimit())
                    .concatMap(this::processBatch)
                    .count()
                    .block(PASS_TIMEOUT);
            if (processed != null && processed > 0) {
                log.info("Processed {} due email batch(es)", processed);
            }
        } catch (RuntimeException e) {
            log.error("Batch-processing pass failed", e);
        }
    }

    public void retryFailedDeliveries() {
        Instant cutoff = Instant.now().minus(emailProperties.retryWindow());
        try {
            Long retried = emailLogRepository
                    .claimRetryable(cutoff, schedulerProperties.claimLimit())
                    .concatMap(emailSendService::retry)
                    .count()
                    .block(PASS_TIMEOUT);
            if (retried != null && retried > 0) {
                log.info("Retried {} failed email(s)", retried);
            }
        } catch (RuntimeException e) {
            log.error("Retry pass failed", e);
        }
    }

    public void exhaustExpiredRetries() {
        Instant cutoff = Instant.now().minus(emailProperties.retryWindow());
        try {
            List<EmailLog> exhausted =
                    emailLogRepository.markExhausted(cutoff).collectList().block(PASS_TIMEOUT);
            if (exhausted != null) {
                // Requirement: after 7 days stop retrying and log the failure for follow-up.
                exhausted.forEach(row -> log.warn(
                        "Email delivery EXHAUSTED after retry window — email_log {} to {} (first attempt {}, "
                                + "{} attempts, last reason: {})",
                        row.id(),
                        row.recipientEmail(),
                        row.firstAttemptedAt(),
                        row.attemptCount(),
                        row.failureReason()));
            }
        } catch (RuntimeException e) {
            log.error("Exhaustion pass failed", e);
        }
    }

    private Mono<EmailBatch> processBatch(EmailBatch batch) {
        return emailSendService
                .send(EmailMapper.toSendRequest(batch), batch.id())
                .map(parts -> {
                    boolean allSent = parts.stream().allMatch(part -> part.status() == EmailStatus.SENT);
                    String reason = allSent
                            ? null
                            : parts.stream()
                                    .filter(part -> part.status() != EmailStatus.SENT)
                                    .map(EmailLog::failureReason)
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse("one or more parts failed to send");
                    return batch.asProcessed(
                            allSent ? BatchStatus.COMPLETED : BatchStatus.FAILED, reason, Instant.now());
                })
                .onErrorResume(e -> {
                    log.error("Email batch {} failed before dispatch", batch.id(), e);
                    return Mono.just(
                            batch.asProcessed(BatchStatus.FAILED, EmailSendService.failureReason(e), Instant.now()));
                })
                .flatMap(batchRepository::save);
    }
}
