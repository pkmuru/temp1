package com.ubs.wma.aat.rampuppack.repository;

import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailBatch;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

public interface EmailBatchRepository extends ReactiveCrudRepository<EmailBatch, Long> {

    Flux<EmailBatch> findByStatus(BatchStatus status);

    /**
     * Atomically claims due batch rows for processing ({@code SCHEDULED} → {@code PROCESSING})
     * and returns them. {@code FOR UPDATE SKIP LOCKED} makes concurrent pollers (multiple
     * instances) claim disjoint rows — no row is ever processed twice.
     */
    @Query("""
            UPDATE aat_app.email_batch
               SET status = 'PROCESSING', updated_at = now()
             WHERE id IN (SELECT id FROM aat_app.email_batch
                           WHERE status = 'SCHEDULED' AND scheduled_at <= now()
                           ORDER BY scheduled_at
                           LIMIT :limit
                           FOR UPDATE SKIP LOCKED)
            RETURNING *
            """)
    Flux<EmailBatch> claimDue(int limit);
}
