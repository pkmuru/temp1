package com.ubs.wma.aat.rampuppack.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Microsoft Entra ID credential settings for the service's own identity (SPN).
 *
 * <p>When {@code tenantId}, {@code clientId} and {@code clientSecret} are all present, an
 * explicit {@code ClientSecretCredential} (Service Principal) is used. When only
 * {@code clientId} is present, it selects the User-Assigned Managed Identity (UAMI) with that
 * client id. Otherwise the service falls back to {@code DefaultAzureCredential}
 * (system-assigned identity in Azure, Azure CLI login locally).
 */
@ConfigurationProperties(prefix = "app.azure.credential")
public record AzureCredentialProperties(String tenantId, String clientId, String clientSecret) {}
