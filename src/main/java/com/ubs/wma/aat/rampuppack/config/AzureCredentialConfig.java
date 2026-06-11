package com.ubs.wma.aat.rampuppack.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.ubs.wma.aat.rampuppack.config.properties.AzureCredentialProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Provides the single {@link TokenCredential} that represents this service's Azure identity.
 * It is reused both for StaatEmail calls and for Entra passwordless database access.
 */
@Configuration(proxyBeanMethods = false)
public class AzureCredentialConfig {

    @Bean
    public TokenCredential azureTokenCredential(AzureCredentialProperties props) {
        if (StringUtils.hasText(props.tenantId())
                && StringUtils.hasText(props.clientId())
                && StringUtils.hasText(props.clientSecret())) {
            // Explicit Service Principal (SPN) credentials.
            return new ClientSecretCredentialBuilder()
                    .tenantId(props.tenantId())
                    .clientId(props.clientId())
                    .clientSecret(props.clientSecret())
                    .build();
        }
        // Managed identity in Azure; AZURE_* env vars or Azure CLI locally.
        return new DefaultAzureCredentialBuilder().build();
    }
}
