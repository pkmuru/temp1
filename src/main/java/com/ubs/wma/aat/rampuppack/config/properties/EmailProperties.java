package com.ubs.wma.aat.rampuppack.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Email delivery tuning.
 *
 * <p>{@code maxAttachmentBytes} is the per-email attachment budget (default 35 MB — the Microsoft
 * Outlook limit); a send whose retention packs exceed it is split into sequential "Part X of N"
 * emails. {@code retryWindow} bounds automatic retries of failed deliveries, measured from the
 * first attempt (default 7 days); after it elapses the log row is marked {@code EXHAUSTED}.
 */
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
        @DefaultValue("36700160") long maxAttachmentBytes,
        @DefaultValue("P7D") Duration retryWindow) {
}
