package com.ubs.wma.aat.rampuppack.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.ubs.wma.aat.rampuppack.config.properties.AzureCredentialProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Provides the single {@link TokenCredential} that represents this service's Azure identity.
 * It is reused both for StaatEmail calls and for Entra passwordless database access.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>tenant-id + client-id + client-secret all set → explicit SPN ({@code ClientSecretCredential});</li>
 *   <li>client-id only → User-Assigned Managed Identity (UAMI) with that client id, via
 *       {@code DefaultAzureCredential} (how the service runs when hosted in Azure);</li>
 *   <li>nothing set → plain {@code DefaultAzureCredential} (system-assigned identity in Azure,
 *       {@code az login} user on a developer machine).</li>
 * </ol>
 */
@Configuration(proxyBeanMethods = false)
public class AzureCredentialConfig {

    private static final Logger log = LoggerFactory.getLogger(AzureCredentialConfig.class);

    @Bean
    public TokenCredential azureTokenCredential(AzureCredentialProperties props) {
        if (StringUtils.hasText(props.tenantId())
                && StringUtils.hasText(props.clientId())
                && StringUtils.hasText(props.clientSecret())) {
            // Explicit Service Principal (SPN) credentials.
            log.info("Azure identity mode: SPN (ClientSecretCredential), client-id '{}'", props.clientId());
            return new ClientSecretCredentialBuilder()
                    .tenantId(props.tenantId())
                    .clientId(props.clientId())
                    .clientSecret(props.clientSecret())
                    .build();
        }
        DefaultAzureCredentialBuilder builder = new DefaultAzureCredentialBuilder();
        if (StringUtils.hasText(props.clientId())) {
            // User-Assigned Managed Identity: select WHICH identity by its client id.
            log.info("Azure identity mode: User-Assigned Managed Identity (UAMI), client-id '{}'", props.clientId());
            builder.managedIdentityClientId(props.clientId());
        } else {
            log.info("Azure identity mode: DefaultAzureCredential chain "
                    + "(system-assigned identity in Azure, az-login user on a developer machine)");
        }
        return builder.build();
    }
}
