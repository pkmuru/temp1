# ramp-up-pack-service

Reactive REST microservice for **WMA AAT ramp-up packs**, built on the 2026 Spring stack.

| Concern | Choice |
|---|---|
| Language | **Java 21** (LTS) |
| Framework | **Spring Boot 4.1.0** / Spring Framework 7.x |
| Web | **Spring WebFlux** (reactive, Netty) |
| Data | **Spring Data R2DBC** + `r2dbc-postgresql` + `r2dbc-pool` (fully non-blocking) |
| Database | **Azure Database for PostgreSQL**, **Microsoft Entra ID passwordless** auth |
| Identity | **azure-identity** SPN / managed identity (StaatEmail calls + DB token) |
| API docs | **springdoc-openapi 3.0.3** (OpenAPI 3.1 + Swagger UI) |
| Build | **Maven** (`com.ubs.wma.aat:ramp-up-pack-service`) |
| Config | `application.yaml` + `bootstrap.yaml` (no Spring Cloud) |

> Inbound requests are **not** authenticated (by design). JSON is handled by **Jackson 3**, the new Boot 4 default.

## Project layout

Layered (controller / service / repository) package structure:

```
src/main/java/com/ubs/wma/aat/rampuppack
├── RampUpPackServiceApplication.java     # entry point
├── controller/                           # REST controllers (WebFlux annotated endpoints)
├── service/                              # business logic
├── repository/                           # Spring Data R2DBC repositories
├── domain/                               # entities (records) + enums
├── dto/                                  # API request/response records
├── mapper/                               # entity ↔ DTO mapping
├── exception/                            # ProblemDetail (RFC 9457) exception handling
├── config/                               # OpenAPI, Azure credential, StaatEmail WebClient, R2DBC passwordless
│   └── properties/                       # typed @ConfigurationProperties
├── datasource/                           # EntraTokenAuthConnectionFactory (token-as-password)
└── client/staatemail/                    # StaatEmail @HttpExchange client + SPN Bearer filter
src/main/resources
├── application.yaml                      # main config (spring.sql.init.mode=never — the app never runs DDL)
└── bootstrap.yaml                        # early/identity config (imported first)
src/test/resources
└── schema.sql                            # table DDL — used by TESTS ONLY (embedded PostgreSQL)
```

## Prerequisites

- JDK 21
- Maven 3.9+
- **No Docker needed** — integration tests run real PostgreSQL 16 via Zonky embedded-postgres (local subprocess)

## Run the tests

```bash
mvn test
```

Integration tests use **Zonky embedded PostgreSQL** (real PG 16 binaries as a local subprocess —
no Docker daemon) and Entra passwordless is disabled for tests, so **no Azure access is required**.
See `AbstractIntegrationTest`.

- `RampUpPackServiceApplicationTests` — full-context end-to-end (WebFlux → R2DBC → Postgres, OpenAPI, actuator, validation)
- `RampUpPackControllerTest` — `@WebFluxTest` slice with a mocked service
- `RampUpPackRepositoryTest` — `@DataR2dbcTest` slice against PostgreSQL
- `EntraBearerExchangeFilterTest` — unit test of the SPN Bearer-token filter

To explore the same embedded database interactively (psql/DBeaver, no Docker), run
`EmbeddedPostgresRunner` from `src/test` — it starts PG on a fixed port `5433` and keeps it alive.

## Run locally (plain PostgreSQL, no Azure)

Disable passwordless and point at a local database:

```bash
docker run --rm -e POSTGRES_DB=rampup -e POSTGRES_USER=rampup -e POSTGRES_PASSWORD=rampup -p 5432:5432 postgres:16-alpine

# Apply the schema yourself first (the app never runs DDL):
psql -h localhost -U rampup rampup -f src/test/resources/schema.sql

DB_PASSWORDLESS_ENABLED=false \
DB_URL='r2dbc:postgresql://localhost:5432/rampup' \
DB_ADMIN_USERNAME=rampup DB_ADMIN_PASSWORD=rampup \
mvn spring-boot:run
```

No Docker? `EmbeddedPostgresRunner` (in `src/test`) starts a local PostgreSQL on port 5433 with the
schema pre-applied — point the app at it the same way.

Then browse:

- Swagger UI → http://localhost:8080/swagger-ui.html
- OpenAPI spec → http://localhost:8080/v3/api-docs
- Health → http://localhost:8080/actuator/health

```bash
curl -X POST http://localhost:8080/api/v1/ramp-up-packs \
  -H 'Content-Type: application/json' \
  -d '{"name":"APAC Advisor Onboarding","description":"Pack for APAC advisors","status":"DRAFT"}'
curl http://localhost:8080/api/v1/ramp-up-packs
```

## Run against Azure (Entra passwordless + SPN)

Passwordless DB auth is **on by default**. Provide the service identity and database coordinates.

| Variable | Purpose |
|---|---|
| `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET` | SPN credentials. Omit all three to use `DefaultAzureCredential` (managed identity in Azure). |
| `DB_HOST` | e.g. `my-server.postgres.database.azure.com` |
| `DB_NAME` | database name |
| `DB_USERNAME` | the PostgreSQL role mapped to the SPN / managed identity |
| `DB_SSL_MODE` | `require` (default) or `verify-full` |
| `STAATEMAIL_BASE_URL` | base URL of the StaatEmail Entra-secured API |
| `STAATEMAIL_SCOPE` | e.g. `api://<staatemail-app-id>/.default` |

The DB access token is requested for scope `https://ossrdbms-aad.database.windows.net/.default`
and used as the PostgreSQL password (see `EntraTokenAuthConnectionFactory`). StaatEmail calls get a
Bearer token for `STAATEMAIL_SCOPE` via `EntraBearerExchangeFilter`.

> The application **never executes SQL/DDL scripts** (`spring.sql.init.mode=never`). The schema is
> managed externally (DBA / deployment pipeline / migration tooling). `schema.sql` lives in
> `src/test/resources` and is applied only to the embedded test database.

## Build & containerize

```bash
mvn clean package
docker build -t ramp-up-pack-service:0.0.1 .
docker run --rm -p 8080:8080 ramp-up-pack-service:0.0.1
```

## Notes on versions

- **Spring Cloud is intentionally not included.** `bootstrap.yaml` is a plain config file imported
  first (lower precedence than `application.yaml`). To adopt a real Spring Cloud Config bootstrap,
  pin to **Spring Boot 4.0.x GA + Spring Cloud 2025.1.x “Oakwood”** (the verified pairing) and add
  `spring-cloud-starter-bootstrap`.
- `spring-boot-starter-parent:4.1.0` is the GA release (on Maven Central — no milestone repo needed).
  `r2dbc-postgresql` and `r2dbc-pool` versions are managed by the Boot BOM; only `springdoc-openapi`
  (3.0.3) and `azure-identity` (1.18.3) are pinned explicitly.
