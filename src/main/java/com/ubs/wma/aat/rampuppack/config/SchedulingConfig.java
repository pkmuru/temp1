package com.ubs.wma.aat.rampuppack.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} processing (the email delivery scheduler) unless
 * {@code app.scheduler.enabled=false} — tests disable it and drive the passes directly.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
