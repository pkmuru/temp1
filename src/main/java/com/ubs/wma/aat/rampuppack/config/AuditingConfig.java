package com.ubs.wma.aat.rampuppack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.util.StringUtils;
import org.springframework.web.server.WebFilter;

import reactor.core.publisher.Mono;

/**
 * Supplies the auditor for {@code @CreatedBy}/{@code @LastModifiedBy} columns. The service has no
 * inbound auth, so callers identify themselves via the {@value #USER_ID_HEADER} request header: a
 * {@link WebFilter} copies it into the Reactor context, and the {@link ReactiveAuditorAware} bean
 * reads it back when an entity is persisted. Writes outside a request (scheduler) and requests
 * without the header audit as {@value #SYSTEM_AUDITOR}.
 */
@Configuration(proxyBeanMethods = false)
public class AuditingConfig {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String SYSTEM_AUDITOR = "system";

    private static final String CONTEXT_KEY = "rampuppack.auditUser";

    @Bean
    public WebFilter userIdContextFilter() {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
            Mono<Void> filtered = chain.filter(exchange);
            return StringUtils.hasText(userId)
                    ? filtered.contextWrite(context -> context.put(CONTEXT_KEY, userId))
                    : filtered;
        };
    }

    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        return () -> Mono.deferContextual(context ->
                Mono.just(context.getOrDefault(CONTEXT_KEY, SYSTEM_AUDITOR)));
    }
}
