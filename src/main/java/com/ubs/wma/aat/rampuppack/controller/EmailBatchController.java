package com.ubs.wma.aat.rampuppack.controller;

import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.dto.EmailBatchRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailBatchResponse;
import com.ubs.wma.aat.rampuppack.mapper.EmailMapper;
import com.ubs.wma.aat.rampuppack.service.EmailBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Scheduled (batched) sends: same payload as a live send plus a schedule date/time. */
@RestController
@RequestMapping("/api/v1/email-batches")
@Tag(name = "Email Batches", description = "Queue retention-pack emails for the scheduler to deliver")
public class EmailBatchController {

    private final EmailBatchService service;

    public EmailBatchController(EmailBatchService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add a send request to the batch",
            description = "The scheduler picks the entry up once scheduledAt is due (a past value "
                    + "is processed on the next poll).")
    public Mono<EmailBatchResponse> add(@Valid @RequestBody EmailBatchRequest request) {
        return service.schedule(EmailMapper.toEntity(request)).map(EmailMapper::toResponse);
    }

    @GetMapping
    @Operation(summary = "List batch entries", description = "Optionally filtered by status.")
    public Flux<EmailBatchResponse> list(@RequestParam(required = false) BatchStatus status) {
        return service.findAll(status).map(EmailMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a batch entry by id")
    public Mono<EmailBatchResponse> get(@PathVariable Long id) {
        return service.findById(id).map(EmailMapper::toResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancel a batch entry",
            description = "Only possible while the entry is still SCHEDULED; returns the cancelled entry.")
    public Mono<EmailBatchResponse> cancel(@PathVariable Long id) {
        return service.cancel(id).map(EmailMapper::toResponse);
    }
}
