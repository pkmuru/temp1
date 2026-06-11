package com.ubs.wma.aat.rampuppack.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for <strong>StaatEmail</strong> — the firm-wide email REST service that this
 * service calls using its SPN. {@code scope} is the client-credentials scope of the StaatEmail app
 * registration, typically {@code api://<app-id>/.default}.
 */
@ConfigurationProperties(prefix = "app.staatemail")
public record StaatEmailProperties(
        String baseUrl,
        String scope) {
}
