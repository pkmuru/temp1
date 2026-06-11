package com.ubs.wma.aat.rampuppack.service;

import com.ubs.wma.aat.rampuppack.domain.RampUpPack;
import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;
import com.ubs.wma.aat.rampuppack.exception.ResourceNotFoundException;
import com.ubs.wma.aat.rampuppack.repository.RampUpPackRepository;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Business logic for ramp-up packs. Enforces existence and applies update semantics, keeping the
 * controller and repository thin. Identity is assigned by the database and the
 * {@code createdAt}/{@code updatedAt} timestamps by Spring Data auditing (see {@code R2dbcConfig}).
 */
@Service
public class RampUpPackService {

    private final RampUpPackRepository repository;

    public RampUpPackService(RampUpPackRepository repository) {
        this.repository = repository;
    }

    public Flux<RampUpPack> findAll() {
        return repository.findAll();
    }

    public Flux<RampUpPack> findByStatus(RampUpPackStatus status) {
        return repository.findByStatus(status);
    }

    public Mono<RampUpPack> findById(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("RampUpPack", id)));
    }

    public Mono<RampUpPack> create(RampUpPack pack) {
        return repository.save(pack);
    }

    public Mono<RampUpPack> update(Long id, RampUpPack changes) {
        return findById(id).flatMap(existing -> repository.save(
                existing.withChanges(changes.name(), changes.description(), changes.status())));
    }

    public Mono<Void> delete(Long id) {
        return findById(id).flatMap(repository::delete);
    }
}
