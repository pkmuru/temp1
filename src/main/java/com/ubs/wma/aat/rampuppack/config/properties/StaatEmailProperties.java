package com.ubs.wma.aat.rampuppack.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/**
 * Configuration for <strong>StaatEmail</strong> — the firm-wide email REST service.
 *
 * <p>StaatEmail is protected by Microsoft Entra ID: this service authenticates with its own
 * dedicated SPN ({@code tenantId}/{@code clientId}/{@code clientSecret}) using the OAuth2
 * <em>client-credentials</em> flow (MSAL confidential client under the hood) and sends the
 * resulting token as a Bearer header. {@code scope} is the client-credentials scope of the
 * StaatEmail app registration, typically {@code api://<app-id>/.default}. When the SPN values are
 * not set (local dev), the application's shared Azure credential is used instead.
 *
 * <p>The sender identity fields ({@code senderGpn}, {@code fromGpn}, addresses) and
 * {@code applicationName} are stamped on every {@code /sendEmail} request.
 */
@ConfigurationProperties(prefix = "app.staatemail")
public record StaatEmailProperties(
        String baseUrl,
        String scope,
        String tenantId,
        String clientId,
        String clientSecret,
        String senderGpn,
        String fromGpn,
        String fromAddress,
        String senderAddress,
        String replyTo,
        @DefaultValue("AAT") String applicationName,
        @DefaultValue("false") boolean wiretap) {

    /** True when the dedicated StaatEmail SPN is fully configured. */
    public boolean hasDedicatedSpn() {
        return StringUtils.hasText(tenantId) && StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }
}
