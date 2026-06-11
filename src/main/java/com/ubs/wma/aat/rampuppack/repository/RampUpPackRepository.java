package com.ubs.wma.aat.rampuppack.repository;

import com.ubs.wma.aat.rampuppack.domain.RampUpPack;
import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

/**
 * Reactive Spring Data R2DBC repository for {@link RampUpPack}.
 */
public interface RampUpPackRepository extends ReactiveCrudRepository<RampUpPack, Long> {

    /** Derived query: all packs with the given lifecycle status. */
    Flux<RampUpPack> findByStatus(RampUpPackStatus status);
}
