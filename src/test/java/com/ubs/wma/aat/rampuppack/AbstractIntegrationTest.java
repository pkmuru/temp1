package com.ubs.wma.aat.rampuppack;

import java.io.IOException;
import java.io.UncheckedIOException;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests that need a real PostgreSQL.
 *
 * <p>Uses <strong>Zonky embedded-postgres</strong> — actual PostgreSQL 16 binaries run as a local
 * subprocess, with <em>no Docker required</em>. One instance is started per JVM and shared across
 * subclasses. Its coordinates are published via {@link DynamicPropertySource} into
 * {@code spring.r2dbc.*} (with Entra passwordless disabled), so Spring Boot's standard R2DBC
 * auto-configuration connects to it.
 *
 * <p><strong>Safety:</strong> these overrides are unconditional — every test extending this class
 * talks ONLY to the embedded localhost instance, never to an Azure PostgreSQL. SQL init (schema.sql,
 * from src/test/resources) is enabled here for tests only; the application itself never runs DDL
 * ({@code spring.sql.init.mode=never} in application.yaml).
 */
public abstract class AbstractIntegrationTest {

    protected static final EmbeddedPostgres POSTGRES = startEmbeddedPostgres();

    private static EmbeddedPostgres startEmbeddedPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to start embedded PostgreSQL", e);
        }
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("app.datasource.passwordless-enabled", () -> false);
        registry.add("spring.r2dbc.url",
                () -> "r2dbc:postgresql://localhost:" + POSTGRES.getPort() + "/postgres");
        registry.add("spring.r2dbc.username", () -> "postgres");
        registry.add("spring.r2dbc.password", () -> "postgres");
        registry.add("spring.sql.init.mode", () -> "always");
    }
}
