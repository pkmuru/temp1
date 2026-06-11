package com.ubs.wma.aat.rampuppack.config;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.ubs.wma.aat.rampuppack.config.properties.DatasourceProperties;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.client.SSLMode;
import io.r2dbc.spi.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Builds the reactive PostgreSQL {@link ConnectionFactory} using Microsoft Entra ID
 * passwordless authentication, wrapped in an R2DBC connection pool.
 *
 * <p>Follows the Microsoft-recommended token-as-password pattern for Azure Database for
 * PostgreSQL: the driver's native dynamic-password support ({@code password(Publisher)} —
 * designed for Entra/IAM token rotation) re-evaluates the token {@link Mono} for every new
 * physical connection, fully non-blocking. The azure-identity SDK caches and proactively
 * refreshes tokens, so Entra is not called per connection. (Spring Cloud Azure's passwordless
 * starter covers JDBC only; this is the equivalent for R2DBC.)
 *
 * <p>Active only when {@code app.datasource.passwordless-enabled=true} (the default). When it is
 * {@code false} — e.g. local development against a plain PostgreSQL, or tests using the Zonky
 * embedded PostgreSQL — this backs off and Spring Boot's own R2DBC auto-configuration provides
 * the {@code ConnectionFactory} from {@code spring.r2dbc.*}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.datasource.passwordless-enabled", havingValue = "true", matchIfMissing = true)
public class R2dbcPasswordlessConfig {

    private static final Logger log = LoggerFactory.getLogger(R2dbcPasswordlessConfig.class);

    @Bean
    public ConnectionFactory connectionFactory(TokenCredential azureTokenCredential, DatasourceProperties props) {
        TokenRequestContext tokenRequest = new TokenRequestContext().addScopes(props.entraScope());
        Mono<CharSequence> entraTokenPassword = Mono.defer(() -> azureTokenCredential
                .getToken(tokenRequest)
                .doOnNext(token -> log.info(
                        "Entra DB token acquired for principal '{}' (expires {}), connecting as PG role '{}'",
                        principalOf(token.getToken()),
                        token.getExpiresAt(),
                        props.username()))
                .doOnError(e -> log.error("Entra DB token acquisition FAILED: {}", e.getMessage()))
                .map(AccessToken::getToken));

        // Eager startup check (non-blocking, fire-and-forget): acquires one token immediately so a
        // misconfigured identity is visible in the log at startup instead of on the first request.
        entraTokenPassword.subscribe(token -> {}, e -> {});

        PostgresqlConnectionConfiguration configuration = PostgresqlConnectionConfiguration.builder()
                .host(props.host())
                .port(props.port())
                .database(props.database())
                .username(props.username())
                .password(entraTokenPassword)
                // "require" forces TLS (mandatory on Azure).
                .sslMode(SSLMode.fromValue(props.sslMode()))
                .build();

        DatasourceProperties.Pool pool = props.pool();
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(
                        new PostgresqlConnectionFactory(configuration))
                .initialSize(pool.initialSize())
                .maxSize(pool.maxSize())
                .maxIdleTime(pool.maxIdleTime())
                .build();

        return new ConnectionPool(poolConfig);
    }

    /**
     * Extracts the human-readable identity from the token's JWT claims for logging — the user UPN
     * ({@code upn}/{@code preferred_username}), or the application/object id for an SPN or managed
     * identity. Never logs the token itself.
     */
    static String principalOf(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return "<unknown>";
            }
            String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            for (String claim : List.of("upn", "preferred_username", "unique_name", "appid", "oid")) {
                Matcher matcher = Pattern.compile("\"" + claim + "\"\\s*:\\s*\"([^\"]+)\"")
                        .matcher(claims);
                if (matcher.find()) {
                    return matcher.group(1) + " (" + claim + ")";
                }
            }
            return "<unknown>";
        } catch (RuntimeException e) {
            return "<unknown>";
        }
    }
}
