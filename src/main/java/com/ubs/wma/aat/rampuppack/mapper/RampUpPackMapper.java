package com.ubs.wma.aat.rampuppack.mapper;

import com.ubs.wma.aat.rampuppack.domain.RampUpPack;
import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;
import com.ubs.wma.aat.rampuppack.dto.RampUpPackRequest;
import com.ubs.wma.aat.rampuppack.dto.RampUpPackResponse;

/**
 * Maps between the {@link RampUpPack} entity and its API DTOs.
 */
public final class RampUpPackMapper {

    static final RampUpPackStatus DEFAULT_STATUS = RampUpPackStatus.DRAFT;

    private RampUpPackMapper() {
    }

    /** Builds a transient entity from a request (id/timestamps are assigned on persist). */
    public static RampUpPack toEntity(RampUpPackRequest request) {
        RampUpPackStatus status = request.status() != null ? request.status() : DEFAULT_STATUS;
        return RampUpPack.newPack(request.name(), request.description(), status);
    }

    public static RampUpPackResponse toResponse(RampUpPack pack) {
        return new RampUpPackResponse(
                pack.id(),
                pack.name(),
                pack.description(),
                pack.status(),
                pack.createdAt(),
                pack.updatedAt());
    }
}
