package com.ubs.wma.aat.rampuppack.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ubs.wma.aat.rampuppack.domain.StaatInsightDocument;
import com.ubs.wma.aat.rampuppack.exception.MissingInsightDocumentException;
import com.ubs.wma.aat.rampuppack.exception.ResourceNotFoundException;
import com.ubs.wma.aat.rampuppack.repository.StaatInsightDocumentRepository;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Read-only access to STAAT insight documents (retention packs) — the table is populated
 * externally by DS/datamesh, so there are no write operations.
 */
@Service
public class InsightDocumentService {

    private final StaatInsightDocumentRepository repository;

    public InsightDocumentService(StaatInsightDocumentRepository repository) {
        this.repository = repository;
    }

    public Mono<StaatInsightDocument> findByAceId(String aceId) {
        return repository.findByAceId(aceId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("STAAT insight document for ACE id", aceId)));
    }

    public Flux<StaatInsightDocument> findByAceIds(Collection<String> aceIds) {
        return repository.findByAceIdIn(aceIds);
    }

    /**
     * All documents for the given ACE ids in request order (duplicates collapsed); fails with
     * {@link MissingInsightDocumentException} when any id has no document — a send is
     * all-or-nothing per request.
     */
    public Mono<List<StaatInsightDocument>> requireAll(List<String> aceIds) {
        List<String> distinct = aceIds.stream().distinct().toList();
        return repository.findByAceIdIn(distinct)
                .collectMap(StaatInsightDocument::aceId)
                .flatMap(byAceId -> {
                    List<String> missing = distinct.stream()
                            .filter(aceId -> !byAceId.containsKey(aceId))
                            .toList();
                    return missing.isEmpty()
                            ? Mono.just(orderedDocuments(distinct, byAceId))
                            : Mono.error(new MissingInsightDocumentException(missing));
                });
    }

    private static List<StaatInsightDocument> orderedDocuments(List<String> aceIds,
                                                               Map<String, StaatInsightDocument> byAceId) {
        return aceIds.stream().map(byAceId::get).toList();
    }
}
