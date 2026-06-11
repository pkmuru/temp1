package com.ubs.wma.aat.rampuppack.client.staatemail;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import reactor.core.publisher.Mono;

/**
 * Declarative HTTP client for <strong>StaatEmail</strong> — the firm-wide email REST service
 * (Microsoft Entra ID / SPN secured).
 *
 * <p>Backed by a {@code WebClient} that attaches this service's SPN Bearer token (see
 * {@code StaatEmailConfig}); callers do not deal with tokens. Replace {@link #ping()} with the real
 * StaatEmail contract — add methods annotated with {@code @GetExchange}/{@code @PostExchange}.
 */
@HttpExchange
public interface StaatEmailClient {

    /** Sample call against StaatEmail. */
    @GetExchange("/ping")
    Mono<String> ping();
}
