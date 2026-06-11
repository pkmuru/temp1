package com.ubs.wma.aat.rampuppack.controller;

import java.util.List;

import com.ubs.wma.aat.rampuppack.dto.InsightDocumentResponse;
import com.ubs.wma.aat.rampuppack.mapper.InsightDocumentMapper;
import com.ubs.wma.aat.rampuppack.service.InsightDocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Read-only API for STAAT insight documents (retention packs) — the documents are produced
 * externally (DS/datamesh), so retrieval by ACE id is all this service offers.
 */
@RestController
@RequestMapping("/api/v1/insight-documents")
@Tag(name = "STAAT Insight Documents", description = "Read-only retention-pack documents, keyed by ACE id")
public class InsightDocumentController {

    private final InsightDocumentService service;

    public InsightDocumentController(InsightDocumentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get insight documents for multiple ACE ids",
            description = "Example: ?aceIds=ACE-1,ACE-2. Unknown ids are simply absent from the result.")
    public Flux<InsightDocumentResponse> listByAceIds(@RequestParam List<String> aceIds) {
        return service.findByAceIds(aceIds).map(InsightDocumentMapper::toResponse);
    }

    @GetMapping("/{aceId}")
    @Operation(summary = "Get the insight document for a single ACE id")
    public Mono<InsightDocumentResponse> getByAceId(@PathVariable String aceId) {
        return service.findByAceId(aceId).map(InsightDocumentMapper::toResponse);
    }
}
