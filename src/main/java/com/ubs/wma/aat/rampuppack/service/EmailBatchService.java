package com.ubs.wma.aat.rampuppack.service;

import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailBatch;
import com.ubs.wma.aat.rampuppack.exception.ConflictException;
import com.ubs.wma.aat.rampuppack.exception.ResourceNotFoundException;
import com.ubs.wma.aat.rampuppack.repository.EmailBatchRepository;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Manages scheduled send requests (the {@code email_batch} table). */
@Service
public class EmailBatchService {

    private final EmailBatchRepository repository;
    private final EmailTemplateService templateService;

    public EmailBatchService(EmailBatchRepository repository, EmailTemplateService templateService) {
        this.repository = repository;
        this.templateService = templateService;
    }

    /** Validates both templates up front so a bad reference fails at enqueue time, not at send time. */
    public Mono<EmailBatch> schedule(EmailBatch batch) {
        return templateService.requireActive(batch.templateId())
                .then(templateService.requireActive(batch.failTemplateId()))
                .then(repository.save(batch));
    }

    public Flux<EmailBatch> findAll(BatchStatus status) {
        return status != null ? repository.findByStatus(status) : repository.findAll();
    }

    public Mono<EmailBatch> findById(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Email batch", id)));
    }

    /** Cancels a batch entry that has not started processing yet. */
    public Mono<EmailBatch> cancel(Long id) {
        return findById(id).flatMap(batch -> batch.status() == BatchStatus.SCHEDULED
                ? repository.save(batch.asCancelled())
                : Mono.error(new ConflictException(
                        "Email batch %d is %s and can no longer be cancelled".formatted(id, batch.status()))));
    }
}
