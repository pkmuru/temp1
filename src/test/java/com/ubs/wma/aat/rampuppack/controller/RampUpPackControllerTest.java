package com.ubs.wma.aat.rampuppack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.ubs.wma.aat.rampuppack.domain.RampUpPack;
import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;
import com.ubs.wma.aat.rampuppack.exception.GlobalExceptionHandler;
import com.ubs.wma.aat.rampuppack.exception.ResourceNotFoundException;
import com.ubs.wma.aat.rampuppack.dto.RampUpPackRequest;
import com.ubs.wma.aat.rampuppack.service.RampUpPackService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Web-layer slice test for {@link RampUpPackController} with a mocked service — no DB, no Azure.
 */
@WebFluxTest(controllers = RampUpPackController.class)
@Import(GlobalExceptionHandler.class)
class RampUpPackControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    RampUpPackService service;

    private RampUpPack sample(Long id) {
        return new RampUpPack(id, "Onboarding", "desc", RampUpPackStatus.DRAFT,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void listReturnsPacks() {
        when(service.findAll()).thenReturn(Flux.just(sample(1L)));

        webTestClient.get().uri("/api/v1/ramp-up-packs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Onboarding");
    }

    @Test
    void getByIdReturnsPack() {
        when(service.findById(1L)).thenReturn(Mono.just(sample(1L)));

        webTestClient.get().uri("/api/v1/ramp-up-packs/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.status").isEqualTo("DRAFT");
    }

    @Test
    void getByUnknownIdReturnsProblemDetail() {
        when(service.findById(404L))
                .thenReturn(Mono.error(new ResourceNotFoundException("RampUpPack", 404L)));

        webTestClient.get().uri("/api/v1/ramp-up-packs/404")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Resource not found");
    }

    @Test
    void createValidPayloadReturnsCreated() {
        when(service.create(any(RampUpPack.class))).thenReturn(Mono.just(sample(7L)));

        webTestClient.post().uri("/api/v1/ramp-up-packs")
                .bodyValue(new RampUpPackRequest("Onboarding", "desc", RampUpPackStatus.DRAFT))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(7);
    }

    @Test
    void createInvalidPayloadReturnsBadRequest() {
        webTestClient.post().uri("/api/v1/ramp-up-packs")
                .bodyValue(new RampUpPackRequest("", null, null))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
