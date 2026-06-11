package com.ubs.wma.aat.rampuppack.repository;

import com.ubs.wma.aat.rampuppack.AbstractIntegrationTest;
import com.ubs.wma.aat.rampuppack.config.AuditingConfig;
import com.ubs.wma.aat.rampuppack.config.R2dbcConfig;

import io.r2dbc.spi.ConnectionFactory;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * Base for R2DBC repository slice tests against the shared embedded PostgreSQL.
 *
 * <p>{@code R2dbcConfig} (enum/JSONB converters, auditing) and {@code AuditingConfig} (the
 * {@code created_by}/{@code updated_by} auditor, "system" outside a web request) are imported
 * because test slices only load data configuration. The schema and seed scripts are idempotent,
 * so re-applying them per test is safe — DDL and seed data live in SQL files
 * ({@code db/schema.sql}, {@code db/seed.sql}), not in Java.
 */
@DataR2dbcTest
@Import({R2dbcConfig.class, AuditingConfig.class})
public abstract class RepositoryTestSupport extends AbstractIntegrationTest {

    @Autowired
    protected ConnectionFactory connectionFactory;

    @BeforeEach
    void applySchemaAndSeed() {
        new ResourceDatabasePopulator(
                new ClassPathResource("db/schema.sql"),
                new ClassPathResource("db/seed.sql"))
                .populate(connectionFactory)
                .block();
    }
}
