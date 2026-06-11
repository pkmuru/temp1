package com.ubs.wma.aat.rampuppack.dto;

import java.time.Instant;

public record EmailTemplateResponse(
        Long id,
        String code,
        String name,
        String description,
        String subject,
        String body,
        boolean active,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt) {}
