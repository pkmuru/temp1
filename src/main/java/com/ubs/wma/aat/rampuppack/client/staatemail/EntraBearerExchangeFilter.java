package com.ubs.wma.aat.rampuppack.client.staatemail;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;

/**
 * Reactive {@code WebClient} filter that obtains a Microsoft Entra ID access token from the
 * configured {@link TokenCredential} (this service's SPN / managed identity) and attaches it
 * as a {@code Bearer} token to every outgoing request to StaatEmail.
 *
 * <p>The azure-identity SDK caches and refreshes tokens internally, so {@code getToken} does
 * not call Entra on every request. The whole flow is non-blocking: {@code getToken} returns a
 * Reactor {@link Mono}.
 */
public class EntraBearerExchangeFilter implements ExchangeFilterFunction {

    private final TokenCredential credential;
    private final TokenRequestContext tokenRequest;

    public EntraBearerExchangeFilter(TokenCredential credential, String scope) {
        this.credential = credential;
        this.tokenRequest = new TokenRequestContext().addScopes(scope);
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return credential.getToken(tokenRequest)
                .map(AccessToken::getToken)
                .map(token -> ClientRequest.from(request)
                        .headers(headers -> headers.setBearerAuth(token))
                        .build())
                .flatMap(next::exchange);
    }
}
