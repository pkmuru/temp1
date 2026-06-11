package com.ubs.wma.aat.rampuppack.repository;

import com.ubs.wma.aat.rampuppack.domain.EmailLog;
import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import java.time.Instant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface EmailLogRepository extends ReactiveCrudRepository<EmailLog, Long> {

    Flux<EmailLog> findByStatus(EmailStatus status);

    Flux<EmailLog> findByBatchId(Long batchId);

    /**
     * Atomically claims failed rows still inside the retry window ({@code FAILED} →
     * {@code SENDING}) and returns them for a retry attempt. {@code cutoff} is
     * {@code now() - retry window}: rows whose first attempt is older than it are exhausted,
     * not retried. {@code FOR UPDATE SKIP LOCKED} keeps concurrent pollers disjoint.
     */
    @Query(
            """
            UPDATE aat_app.email_log
               SET status = 'SENDING', updated_at = now()
             WHERE id IN (SELECT id FROM aat_app.email_log
                           WHERE status = 'FAILED' AND first_attempted_at > :cutoff
                           ORDER BY first_attempted_at
                           LIMIT :limit
                           FOR UPDATE SKIP LOCKED)
            RETURNING *
            """)
    Flux<EmailLog> claimRetryable(Instant cutoff, int limit);

    /**
     * Marks failed rows whose retry window has elapsed as {@code EXHAUSTED} and returns them so
     * the scheduler can log them for follow-up (requirement: cease retries after 7 days).
     */
    @Query(
            """
            UPDATE aat_app.email_log
               SET status = 'EXHAUSTED', updated_at = now()
             WHERE status = 'FAILED' AND first_attempted_at <= :cutoff
            RETURNING *
            """)
    Flux<EmailLog> markExhausted(Instant cutoff);
}
