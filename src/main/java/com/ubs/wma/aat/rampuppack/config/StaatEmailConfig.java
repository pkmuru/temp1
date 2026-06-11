package com.ubs.wma.aat.rampuppack.config;

import com.azure.core.credential.TokenCredential;
import com.ubs.wma.aat.rampuppack.config.properties.StaatEmailProperties;
import com.ubs.wma.aat.rampuppack.client.staatemail.EntraBearerExchangeFilter;
import com.ubs.wma.aat.rampuppack.client.staatemail.StaatEmailClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Wires the <strong>StaatEmail</strong> firm-wide email REST service:
 * <ul>
 *   <li>a reactive {@link WebClient} carrying the {@link EntraBearerExchangeFilter} so the SPN token
 *       is attached automatically;</li>
 *   <li>a declarative {@link StaatEmailClient} HTTP-service client proxied over that {@code WebClient}.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class StaatEmailConfig {

    @Bean
    public WebClient staatEmailWebClient(TokenCredential azureTokenCredential,
                                         StaatEmailProperties props) {
        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .filter(new EntraBearerExchangeFilter(azureTokenCredential, props.scope()))
                .build();
    }

    @Bean
    public StaatEmailClient staatEmailClient(WebClient staatEmailWebClient) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(staatEmailWebClient))
                .build();
        return factory.createClient(StaatEmailClient.class);
    }
}
