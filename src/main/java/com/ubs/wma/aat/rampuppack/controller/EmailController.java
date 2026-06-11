package com.ubs.wma.aat.rampuppack.controller;

import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import com.ubs.wma.aat.rampuppack.dto.EmailLogResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailPreviewResponse;
import com.ubs.wma.aat.rampuppack.dto.EmailSendRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailSendResponse;
import com.ubs.wma.aat.rampuppack.mapper.EmailMapper;
import com.ubs.wma.aat.rampuppack.service.EmailSendService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Live email sending plus the send log (merged values kept for future reference). */
@RestController
@RequestMapping("/api/v1/emails")
@Tag(name = "Emails", description = "Send retention-pack emails and inspect the send log")
public class EmailController {

    private final EmailSendService service;

    public EmailController(EmailSendService service) {
        this.service = service;
    }

    @PostMapping("/send")
    @Operation(summary = "Send retention packs by email now",
            description = "Fetches the STAAT documents for the given ACE ids, merges the template and "
                    + "delivers via StaatEmail — split into sequential parts when attachments exceed "
                    + "the size cap. Failed parts are retried automatically by the scheduler.")
    public Mono<EmailSendResponse> send(@Valid @RequestBody EmailSendRequest request) {
        return service.send(request, null).map(EmailMapper::toSendResponse);
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview a send without delivering it",
            description = "Same payload and pipeline as /send — templates resolved, documents "
                    + "fetched, merge fields applied, size-based splitting — but the merged "
                    + "subject and HTML body are returned to the UI instead of being sent to "
                    + "StaatEmail. Nothing is logged.")
    public Mono<EmailPreviewResponse> preview(@Valid @RequestBody EmailSendRequest request) {
        return service.preview(request);
    }

    @GetMapping
    @Operation(summary = "List sent/attempted emails",
            description = "Optionally filtered by status and/or originating batch.")
    public Flux<EmailLogResponse> list(@RequestParam(required = false) EmailStatus status,
                                       @RequestParam(required = false) Long batchId) {
        return service.findLogs(status, batchId).map(EmailMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one email log entry, including the merged subject/body")
    public Mono<EmailLogResponse> get(@PathVariable Long id) {
        return service.findById(id).map(EmailMapper::toResponse);
    }
}
