package com.ubs.wma.aat.rampuppack.controller;

import com.ubs.wma.aat.rampuppack.domain.RampUpPack;
import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;
import com.ubs.wma.aat.rampuppack.dto.RampUpPackRequest;
import com.ubs.wma.aat.rampuppack.dto.RampUpPackResponse;
import com.ubs.wma.aat.rampuppack.mapper.RampUpPackMapper;
import com.ubs.wma.aat.rampuppack.service.RampUpPackService;

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

/**
 * Reactive REST API for ramp-up packs.
 */
@RestController
@RequestMapping("/api/v1/ramp-up-packs")
@Tag(name = "Ramp-Up Packs", description = "CRUD operations for ramp-up packs")
public class RampUpPackController {

    private final RampUpPackService service;

    public RampUpPackController(RampUpPackService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List ramp-up packs", description = "Returns all packs, optionally filtered by status.")
    public Flux<RampUpPackResponse> list(@RequestParam(required = false) RampUpPackStatus status) {
        Flux<RampUpPack> packs = status != null ? service.findByStatus(status) : service.findAll();
        return packs.map(RampUpPackMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a ramp-up pack by id")
    public Mono<RampUpPackResponse> get(@PathVariable Long id) {
        return service.findById(id).map(RampUpPackMapper::toResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a ramp-up pack")
    public Mono<RampUpPackResponse> create(@Valid @RequestBody RampUpPackRequest request) {
        return service.create(RampUpPackMapper.toEntity(request)).map(RampUpPackMapper::toResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing ramp-up pack")
    public Mono<RampUpPackResponse> update(@PathVariable Long id, @Valid @RequestBody RampUpPackRequest request) {
        return service.update(id, RampUpPackMapper.toEntity(request)).map(RampUpPackMapper::toResponse);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a ramp-up pack")
    public Mono<Void> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}
