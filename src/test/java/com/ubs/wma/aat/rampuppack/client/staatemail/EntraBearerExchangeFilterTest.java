package com.ubs.wma.aat.rampuppack.client.staatemail;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Verifies the SPN token filter attaches a Bearer token from the {@link TokenCredential}.
 * No network or Spring context — the exchange function is stubbed to capture the request.
 */
class EntraBearerExchangeFilterTest {

    @Test
    void attachesBearerTokenFromCredential() {
        TokenCredential credential = request ->
                Mono.just(new AccessToken("test-token-123", OffsetDateTime.now().plusHours(1)));

        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction capturing = request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };

        WebClient client = WebClient.builder()
                .baseUrl("http://staatemail.example")
                .filter(new EntraBearerExchangeFilter(credential, "api://staatemail/.default"))
                .exchangeFunction(capturing)
                .build();

        client.get().uri("/ping").retrieve().bodyToMono(Void.class).block();

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().headers().getFirst("Authorization")).isEqualTo("Bearer test-token-123");
    }
}
