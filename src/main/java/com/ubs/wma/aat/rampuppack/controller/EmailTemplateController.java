package com.ubs.wma.aat.rampuppack.controller;

import com.ubs.wma.aat.rampuppack.dto.EmailTemplateRequest;
import com.ubs.wma.aat.rampuppack.dto.EmailTemplateResponse;
import com.ubs.wma.aat.rampuppack.mapper.EmailTemplateMapper;
import com.ubs.wma.aat.rampuppack.service.EmailTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** CRUD API for email templates. */
@RestController
@RequestMapping("/api/v1/email-templates")
@Tag(name = "Email Templates", description = "Manage the email templates used for retention-pack delivery")
public class EmailTemplateController {

    private final EmailTemplateService service;

    public EmailTemplateController(EmailTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List email templates", description = "All templates, optionally filtered by active flag.")
    public Flux<EmailTemplateResponse> list(@RequestParam(required = false) Boolean active) {
        return service.findAll(active).map(EmailTemplateMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an email template by id")
    public Mono<EmailTemplateResponse> get(@PathVariable Long id) {
        return service.findById(id).map(EmailTemplateMapper::toResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an email template")
    public Mono<EmailTemplateResponse> create(@Valid @RequestBody EmailTemplateRequest request) {
        return service.create(EmailTemplateMapper.toEntity(request)).map(EmailTemplateMapper::toResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing email template")
    public Mono<EmailTemplateResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody EmailTemplateRequest request) {
        return service.update(id, EmailTemplateMapper.toEntity(request)).map(EmailTemplateMapper::toResponse);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an email template",
            description = "Fails with 409 when the template is referenced by sent emails or batches.")
    public Mono<Void> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}
