package com.ubs.wma.aat.rampuppack.repository;

import com.ubs.wma.aat.rampuppack.domain.EmailTemplate;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

public interface EmailTemplateRepository extends ReactiveCrudRepository<EmailTemplate, Long> {

    /** Derived query: templates filtered by their active flag. */
    Flux<EmailTemplate> findByActive(boolean active);
}
