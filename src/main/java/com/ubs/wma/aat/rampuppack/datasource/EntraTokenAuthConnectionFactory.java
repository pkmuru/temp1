package com.ubs.wma.aat.rampuppack.datasource;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * R2DBC {@link ConnectionFactory} that authenticates to Azure Database for PostgreSQL using
 * Microsoft Entra ID instead of a static password.
 *
 * <p>For each new <em>physical</em> connection it requests a fresh access token (default scope
 * {@code https://ossrdbms-aad.database.windows.net/.default}) from the supplied
 * {@link TokenCredential} — the service's SPN or managed identity — and uses the token string
 * as the PostgreSQL password.
 *
 * <p>This is driver-version independent (it only touches the R2DBC SPI) and fully non-blocking:
 * {@link TokenCredential#getToken} returns a Reactor {@link Mono}. Wrap it in a pool so token
 * requests are amortised across pooled connections; the azure-identity SDK also caches tokens.
 */
public class EntraTokenAuthConnectionFactory implements ConnectionFactory {

    private final TokenCredential credential;
    private final ConnectionFactoryOptions baseOptions;
    private final TokenRequestContext tokenRequest;

    /**
     * @param credential  the Azure identity used to acquire database access tokens
     * @param baseOptions connection options <em>without</em> a password (driver, host, port,
     *                    database, user, sslMode)
     * @param entraScope  the token scope for Azure Database for PostgreSQL
     */
    public EntraTokenAuthConnectionFactory(TokenCredential credential,
                                           ConnectionFactoryOptions baseOptions,
                                           String entraScope) {
        this.credential = credential;
        this.baseOptions = baseOptions;
        this.tokenRequest = new TokenRequestContext().addScopes(entraScope);
    }

    @Override
    public Publisher<? extends Connection> create() {
        return credential.getToken(tokenRequest)
                .map(AccessToken::getToken)
                .map(this::withToken)
                .map(ConnectionFactories::get)
                .flatMap(factory -> Mono.from(factory.create()));
    }

    private ConnectionFactoryOptions withToken(String token) {
        return ConnectionFactoryOptions.builder()
                .from(baseOptions)
                .option(ConnectionFactoryOptions.PASSWORD, token)
                .build();
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return ConnectionFactories.get(baseOptions).getMetadata();
    }
}
