package com.ubs.wma.aat.rampuppack.client.staatemail;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * Dev-only request/response wiretap for the StaatEmail {@code WebClient}
 * ({@code app.staatemail.wiretap=true}): logs method, URL, headers, response status and timing
 * for every exchange, plus the error when the exchange itself fails.
 *
 * <p><strong>Credential values are never printed</strong> — repo policy forbids logging secrets
 * and tokens, even in dev. Sensitive headers ({@code Authorization}, cookies) are masked, keeping
 * only the scheme and value length (e.g. {@code Bearer ***(1432 chars)}), which is enough to see
 * whether a token was attached and looks plausibly sized without exposing it.
 */
public class StaatEmailWiretapFilter implements ExchangeFilterFunction {

    private static final Logger log = LoggerFactory.getLogger(StaatEmailWiretapFilter.class);
    private static final Set<String> MASKED_HEADERS =
            Set.of(HttpHeaders.AUTHORIZATION.toLowerCase(), "cookie", "set-cookie", "proxy-authorization");

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        long startNanos = System.nanoTime();
        log.info("StaatEmail >> {} {} headers={}", request.method(), request.url(), redacted(request.headers()));
        return next.exchange(request)
                .doOnNext(response -> log.info(
                        "StaatEmail << {} {} status={} ({} ms) headers={}",
                        request.method(),
                        request.url(),
                        response.statusCode(),
                        elapsedMillis(startNanos),
                        redacted(response.headers().asHttpHeaders())))
                .doOnError(e -> log.warn(
                        "StaatEmail !! {} {} failed after {} ms: {}",
                        request.method(),
                        request.url(),
                        elapsedMillis(startNanos),
                        e.toString()));
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** Header map with credential values masked down to scheme + length. */
    private static String redacted(HttpHeaders headers) {
        StringBuilder out = new StringBuilder("{");
        headers.forEach((name, values) -> {
            if (out.length() > 1) {
                out.append(", ");
            }
            out.append(name).append('=');
            out.append(
                    MASKED_HEADERS.contains(name.toLowerCase())
                            ? values.stream().map(StaatEmailWiretapFilter::mask).toList()
                            : values);
        });
        return out.append('}').toString();
    }

    private static String mask(String value) {
        int schemeEnd = value.indexOf(' ');
        String scheme = schemeEnd > 0 ? value.substring(0, schemeEnd) + " " : "";
        return scheme + "***(" + value.length() + " chars)";
    }
}
