package com.ubs.wma.aat.rampuppack.mapper;

import com.ubs.wma.aat.rampuppack.domain.EmailTemplate;
import com.ubs.wma.aat.rampuppack.dto.EmailTemplateRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailTemplateResponse;

/** Stateless mapping between {@link EmailTemplate} and its API DTOs. */
public final class EmailTemplateMapper {

    private EmailTemplateMapper() {}

    public static EmailTemplate toEntity(EmailTemplateRequest request) {
        return EmailTemplate.newTemplate(
                request.code(),
                request.name(),
                request.description(),
                request.subject(),
                request.body(),
                request.activeOrDefault());
    }

    public static EmailTemplate applyChanges(EmailTemplate existing, EmailTemplateRequest request) {
        return existing.withChanges(
                request.code(),
                request.name(),
                request.description(),
                request.subject(),
                request.body(),
                request.activeOrDefault());
    }

    public static EmailTemplateResponse toResponse(EmailTemplate template) {
        return new EmailTemplateResponse(
                template.id(),
                template.code(),
                template.name(),
                template.description(),
                template.subject(),
                template.body(),
                template.active(),
                template.createdBy(),
                template.updatedBy(),
                template.createdAt(),
                template.updatedAt());
    }
}
