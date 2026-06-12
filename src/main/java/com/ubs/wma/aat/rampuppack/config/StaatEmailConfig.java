package com.ubs.wma.aat.rampuppack.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.ubs.wma.aat.rampuppack.client.staatemail.EntraBearerExchangeFilter;
import com.ubs.wma.aat.rampuppack.client.staatemail.StaatEmailClient;
import com.ubs.wma.aat.rampuppack.client.staatemail.StaatEmailWiretapFilter;
import com.ubs.wma.aat.rampuppack.config.properties.StaatEmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Wires the <strong>StaatEmail</strong> firm-wide email REST service:
 * <ul>
 *   <li>a dedicated SPN credential ({@code app.staatemail.tenant-id/client-id/client-secret})
 *       acquiring tokens via the OAuth2 <em>client-credentials</em> flow — azure-identity's
 *       {@code ClientSecretCredential}, which is MSAL (msal4j confidential client) under the hood,
 *       including token caching and refresh. Falls back to the application's shared Azure
 *       credential when the SPN is not configured (e.g. local dev with {@code az login});</li>
 *   <li>a reactive {@link WebClient} carrying the {@link EntraBearerExchangeFilter} so every
 *       request to StaatEmail gets a {@code Authorization: Bearer <token>} header automatically;</li>
 *   <li>a declarative {@link StaatEmailClient} HTTP-service client proxied over that {@code WebClient}.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class StaatEmailConfig {

    private static final Logger log = LoggerFactory.getLogger(StaatEmailConfig.class);

    @Bean
    public WebClient staatEmailWebClient(TokenCredential azureTokenCredential, StaatEmailProperties props) {
        TokenCredential credential = staatEmailCredential(props, azureTokenCredential);
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(props.baseUrl())
                .filter(new EntraBearerExchangeFilter(credential, props.scope()));
        if (props.wiretap()) {
            // After the bearer filter, so the wiretap sees the (masked) Authorization header.
            // Token values themselves are never logged — hard repo policy, even in dev.
            log.warn("StaatEmail wiretap logging is ON — every request/response is logged (dev use only)");
            builder.filter(new StaatEmailWiretapFilter());
        }
        return builder.build();
    }

    @Bean
    public StaatEmailClient staatEmailClient(WebClient staatEmailWebClient) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(
                        WebClientAdapter.create(staatEmailWebClient))
                .build();
        return factory.createClient(StaatEmailClient.class);
    }

    private static TokenCredential staatEmailCredential(StaatEmailProperties props, TokenCredential sharedCredential) {
        if (props.hasDedicatedSpn()) {
            log.info(
                    "StaatEmail auth: dedicated SPN '{}' via client-credentials flow, scope '{}'",
                    props.clientId(),
                    props.scope());
            return new ClientSecretCredentialBuilder()
                    .tenantId(props.tenantId())
                    .clientId(props.clientId())
                    .clientSecret(props.clientSecret())
                    .build();
        }
        log.info(
                "StaatEmail auth: no dedicated SPN configured — using the application's shared "
                        + "Azure credential, scope '{}'",
                props.scope());
        return sharedCredential;
    }
}
