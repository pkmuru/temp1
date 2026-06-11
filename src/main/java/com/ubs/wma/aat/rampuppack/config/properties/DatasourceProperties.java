package com.ubs.wma.aat.rampuppack.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the reactive PostgreSQL connection when Microsoft Entra ID passwordless
 * authentication is used (see {@code R2dbcPasswordlessConfig}).
 *
 * <p>When {@code passwordlessEnabled} is {@code false}, these are ignored and Spring Boot's
 * standard {@code spring.r2dbc.*} auto-configuration supplies the {@code ConnectionFactory}
 * instead (in tests it points at the Zonky embedded PostgreSQL).
 */
@ConfigurationProperties(prefix = "app.datasource")
public record DatasourceProperties(
        @DefaultValue("true") boolean passwordlessEnabled,
        @DefaultValue("localhost") String host,
        @DefaultValue("5432") Integer port,
        @DefaultValue("postgres") String database,
        @DefaultValue("") String username,
        @DefaultValue("require") String sslMode,
        @DefaultValue("https://ossrdbms-aad.database.windows.net/.default") String entraScope,
        @DefaultValue Pool pool) {

    /** Connection pool sizing for the Entra-authenticated {@code ConnectionFactory}. */
    public record Pool(
            @DefaultValue("5") Integer initialSize,
            @DefaultValue("20") Integer maxSize,
            @DefaultValue("30m") Duration maxIdleTime) {}
}
