package com.ubs.wma.aat.rampuppack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubs.wma.aat.rampuppack.AbstractIntegrationTest;
import com.ubs.wma.aat.rampuppack.config.R2dbcConfig;
import com.ubs.wma.aat.rampuppack.domain.RampUpPack;
import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;

import io.r2dbc.spi.ConnectionFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import reactor.test.StepVerifier;

/**
 * R2DBC slice test for {@link RampUpPackRepository} against a real PostgreSQL container.
 * {@code R2dbcConfig} is imported so the {@link RampUpPackStatus} converters and auditing are
 * active in the slice. The schema is (re)created from schema.sql before each test.
 */
@DataR2dbcTest
@Import(R2dbcConfig.class)
class RampUpPackRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    RampUpPackRepository repository;

    @Autowired
    ConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql"))
                .populate(connectionFactory)
                .block();
        repository.deleteAll().block();
    }

    @Test
    void savesAndAssignsGeneratedIdAndTimestamps() {
        StepVerifier.create(repository.save(RampUpPack.newPack("Pack A", null, RampUpPackStatus.DRAFT)))
                .assertNext(saved -> {
                    assertThat(saved.id()).isNotNull();
                    assertThat(saved.name()).isEqualTo("Pack A");
                    // Populated by Spring Data auditing on insert.
                    assertThat(saved.createdAt()).isNotNull();
                    assertThat(saved.updatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void findByStatusFilters() {
        repository.save(RampUpPack.newPack("A", null, RampUpPackStatus.DRAFT)).block();
        repository.save(RampUpPack.newPack("B", null, RampUpPackStatus.PUBLISHED)).block();

        StepVerifier.create(repository.findByStatus(RampUpPackStatus.PUBLISHED))
                .assertNext(pack -> assertThat(pack.name()).isEqualTo("B"))
                .verifyComplete();
    }
}
