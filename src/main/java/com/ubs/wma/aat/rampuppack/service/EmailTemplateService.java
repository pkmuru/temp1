package com.ubs.wma.aat.rampuppack.service;

import com.ubs.wma.aat.rampuppack.domain.EmailTemplate;
import com.ubs.wma.aat.rampuppack.exception.ConflictException;
import com.ubs.wma.aat.rampuppack.exception.ResourceNotFoundException;
import com.ubs.wma.aat.rampuppack.repository.EmailTemplateRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** CRUD for email templates. */
@Service
public class EmailTemplateService {

    private final EmailTemplateRepository repository;

    public EmailTemplateService(EmailTemplateRepository repository) {
        this.repository = repository;
    }

    public Flux<EmailTemplate> findAll(Boolean active) {
        return active != null ? repository.findByActive(active) : repository.findAll();
    }

    public Mono<EmailTemplate> findById(Long id) {
        return repository.findById(id).switchIfEmpty(Mono.error(new ResourceNotFoundException("Email template", id)));
    }

    /** Resolves a template for sending: it must exist (404) and be active (409). */
    public Mono<EmailTemplate> requireActive(Long id) {
        return findById(id)
                .filter(EmailTemplate::active)
                .switchIfEmpty(Mono.error(new ConflictException(
                        "Email template %d is inactive and cannot be used for sending".formatted(id))));
    }

    public Mono<EmailTemplate> create(EmailTemplate template) {
        return repository.save(template);
    }

    public Mono<EmailTemplate> update(Long id, EmailTemplate changes) {
        return findById(id)
                .flatMap(existing -> repository.save(existing.withChanges(
                        changes.code(),
                        changes.name(),
                        changes.description(),
                        changes.subject(),
                        changes.body(),
                        changes.active())));
    }

    /** Deleting a template referenced by sends/batches fails with a data-conflict (409). */
    public Mono<Void> delete(Long id) {
        return findById(id).flatMap(repository::delete);
    }
}
