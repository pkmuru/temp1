package com.ubs.wma.aat.rampuppack.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Microsoft Entra ID credential settings for the service's own identity (SPN).
 *
 * <p>When {@code tenantId}, {@code clientId} and {@code clientSecret} are all present, an
 * explicit {@code ClientSecretCredential} (Service Principal) is used. Otherwise the service
 * falls back to {@code DefaultAzureCredential} (managed identity in Azure, environment
 * variables / Azure CLI locally).
 */
@ConfigurationProperties(prefix = "app.azure.credential")
public record AzureCredentialProperties(
        String tenantId,
        String clientId,
        String clientSecret) {
}
