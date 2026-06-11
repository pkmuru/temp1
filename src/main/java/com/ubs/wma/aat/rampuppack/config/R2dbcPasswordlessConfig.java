package com.ubs.wma.aat.rampuppack.config;

import com.azure.core.credential.TokenCredential;
import com.ubs.wma.aat.rampuppack.config.properties.DatasourceProperties;
import com.ubs.wma.aat.rampuppack.datasource.EntraTokenAuthConnectionFactory;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the reactive PostgreSQL {@link ConnectionFactory} using Microsoft Entra ID
 * passwordless authentication, wrapped in an R2DBC connection pool.
 *
 * <p>Active only when {@code app.datasource.passwordless-enabled=true} (the default). When it is
 * {@code false} — e.g. local development against a plain PostgreSQL, or tests using the Zonky
 * embedded PostgreSQL — this backs off and Spring Boot's own R2DBC auto-configuration provides
 * the {@code ConnectionFactory} from {@code spring.r2dbc.*}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.datasource.passwordless-enabled", havingValue = "true", matchIfMissing = true)
public class R2dbcPasswordlessConfig {

    @Bean
    public ConnectionFactory connectionFactory(TokenCredential azureTokenCredential,
                                               DatasourceProperties props) {
        ConnectionFactoryOptions baseOptions = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                .option(ConnectionFactoryOptions.HOST, props.host())
                .option(ConnectionFactoryOptions.PORT, props.port())
                .option(ConnectionFactoryOptions.DATABASE, props.database())
                .option(ConnectionFactoryOptions.USER, props.username())
                // PostgreSQL driver option; "require" forces TLS (mandatory on Azure).
                .option(Option.<String>valueOf("sslMode"), props.sslMode())
                .build();

        EntraTokenAuthConnectionFactory entraFactory =
                new EntraTokenAuthConnectionFactory(azureTokenCredential, baseOptions, props.entraScope());

        DatasourceProperties.Pool pool = props.pool();
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(entraFactory)
                .initialSize(pool.initialSize())
                .maxSize(pool.maxSize())
                .maxIdleTime(pool.maxIdleTime())
                .build();

        return new ConnectionPool(poolConfig);
    }
}
