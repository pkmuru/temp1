package com.ubs.wma.aat.rampuppack.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Batch/retry scheduler tuning. {@code pollInterval} (ISO-8601, e.g. {@code PT1H}, {@code P1D})
 * is consumed by {@code @Scheduled(fixedDelayString = "${app.scheduler.poll-interval}")};
 * {@code claimLimit} caps how many rows one poll claims per pass. {@code enabled=false} turns the
 * scheduler off entirely (tests do this).
 */
@ConfigurationProperties(prefix = "app.scheduler")
public record SchedulerProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("PT1H") Duration pollInterval,
        @DefaultValue("25") int claimLimit) {
}
