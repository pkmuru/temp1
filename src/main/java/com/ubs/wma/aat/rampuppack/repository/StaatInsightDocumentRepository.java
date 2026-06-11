package com.ubs.wma.aat.rampuppack.repository;

import java.util.Collection;

import com.ubs.wma.aat.rampuppack.domain.StaatInsightDocument;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Read-only access to STAAT insight documents — rows are populated externally (DS/datamesh);
 * this service never writes to the table.
 */
public interface StaatInsightDocumentRepository extends ReactiveCrudRepository<StaatInsightDocument, Long> {

    Mono<StaatInsightDocument> findByAceId(String aceId);

    Flux<StaatInsightDocument> findByAceIdIn(Collection<String> aceIds);
}
