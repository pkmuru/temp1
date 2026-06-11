# ramp-up-pack-service

Reactive REST microservice for **WMA AAT STAAT retention-pack email delivery**, built on the 2026
Spring stack. When ACE relationships are reassigned after FA attrition, this service emails each
receiving FA (or the Field Leader, for pended relationships) their STAAT retention packs — one
HTML document per ACE relationship, produced externally by DS/datamesh — via the firm **StaatEmail**
API, live or scheduled in batches, with automatic retry of failed deliveries.

| API | Purpose |
|---|---|
| `/api/v1/email-templates` | CRUD for email templates (`{placeholder}` merge fields) |
| `/api/v1/insight-documents` | Read-only STAAT retention packs by ACE id (single or multiple) |
| `/api/v1/emails/send` + `/api/v1/emails` | Live send (split into "Part X of N" above the 35 MB cap) + send log with merged values |
| `/api/v1/emails/preview` | Same payload/pipeline as `/send`, but the merged subject + HTML body go back to the UI — nothing is sent or logged |
| `/api/v1/email-batches` | Queue a send for the scheduler (`scheduledAt`), list/cancel |

Tables are split across two PostgreSQL schemas: **`aat_app`** (owned by this service:
`email_template`, `email_batch`, `email_log`) and **`datamesh`** (externally populated data
products: `staat_insight_document`, SELECT-only for this service).

A configurable scheduler (`app.scheduler.poll-interval`, default hourly) processes due batches,
retries `FAILED` sends for up to 7 days (`app.email.retry-window`) and marks the rest `EXHAUSTED`.
Failure-notification emails (the request's `failTemplateId`) go out on the first failure.

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
└── client/staatemail/                    # StaatEmail @HttpExchange client + SPN Bearer filter
src/main/resources
├── application.yml                       # base defaults (spring.sql.init.mode=never — the app never runs DDL)
├── application-local.yml                 # 'local' profile: local PostgreSQL, passwordless OFF, DevTools
├── application-dev.yml                   # 'dev' profile: Azure-hosted dev, passwordless via UAMI
└── bootstrap.yaml                        # early/identity config (imported first)
src/test/resources
├── application-test.yml                  # 'test' profile: embedded local PostgreSQL (auto-activated by tests)
└── db/
    ├── schema.sql                        # schemas (aat_app, datamesh) + table DDL — TESTS ONLY; DBA handoff reference
    └── seed.sql                          # idempotent reference data (templates + sample insight documents)
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

- `EmailDeliveryFlowTest` — end-to-end delivery flows (send, split, failure + notice, retry,
  preview, batch + scheduler passes) with only the outbound StaatEmail client mocked
- `RampUpPackServiceApplicationTests` — full-context smoke (OpenAPI, actuator, validation,
  auditing via `X-User-Id`, JSONB/array round-trip)
- `controller/*ControllerTest` — `@WebFluxTest` slices with mocked services: routing, status
  codes, bean validation (including the exactly-one-of faId/fieldLeaderId rule), enum
  query-param binding and ProblemDetail error mapping (404/409/422/400/500)
- `repository/*RepositoryTest` — `@DataR2dbcTest` slices against PostgreSQL: claim queries
  (`UPDATE … RETURNING SKIP LOCKED`), JSONB/array mappings, unique constraints, seeded datamesh reads
- `TemplateMergerTest`, `EmailSendServiceSplitTest` — pure unit tests (merge fields, 35 MB splitting)
- `EntraBearerExchangeFilterTest` — unit test of the SPN Bearer-token filter

To explore the same embedded database interactively (psql/DBeaver, no Docker), run
`EmbeddedPostgresRunner` from `src/test` — it starts PG on a fixed port `5433` and keeps it alive.

## Code formatting

Java sources are formatted with [palantir-java-format](https://github.com/palantir/palantir-java-format)
(a lambda/chain-friendly google-java-format fork, 120-column) via the Spotless Maven plugin —
long reactive pipelines break one operator per line.

```bash
mvn spotless:apply   # reformat all sources
mvn spotless:check   # verify only (also runs automatically at the verify phase)
```

For format-on-save in IntelliJ, install the `palantir-java-format` plugin — it produces the same
output as the Maven build.

## Run locally (plain PostgreSQL, no Azure)

Disable passwordless and point at a local database:

```bash
docker run --rm -e POSTGRES_DB=rampup -e POSTGRES_USER=rampup -e POSTGRES_PASSWORD=rampup -p 5432:5432 postgres:16-alpine

# Apply the schema (and optionally the seed) yourself first (the app never runs DDL):
psql -h localhost -U rampup rampup -f src/test/resources/db/schema.sql
psql -h localhost -U rampup rampup -f src/test/resources/db/seed.sql

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
# Create a template ({placeholders} are merged at send time; X-User-Id feeds created_by/updated_by)
curl -X POST http://localhost:8080/api/v1/email-templates \
  -H 'Content-Type: application/json' -H 'X-User-Id: muru' \
  -d '{"code":"FA_DELIVERY_SUCCESS","name":"Successful Delivery - FA",
       "subject":"STAAT Client Retention Packs",
       "body":"<p>Hi {faName},</p><p>{packCount} packs attached.</p>{householdTable}"}'

# Retention packs for one or many ACE ids (read-only; populated externally)
curl 'http://localhost:8080/api/v1/insight-documents/ACE-1001'
curl 'http://localhost:8080/api/v1/insight-documents?aceIds=ACE-1001,ACE-1002'

# Send now (exactly one of faId / fieldLeaderId)
curl -X POST http://localhost:8080/api/v1/emails/send \
  -H 'Content-Type: application/json' \
  -d '{"aceIds":["ACE-1001","ACE-1002"],"faId":"FA-42","templateId":1,"failTemplateId":3,
       "recipientEmail":"fa42@ubs.com","mergeFields":{"faName":"Alex Advisor"}}'

# ...or queue it for the scheduler
curl -X POST http://localhost:8080/api/v1/email-batches \
  -H 'Content-Type: application/json' \
  -d '{"aceIds":["ACE-1003"],"fieldLeaderId":"FL-7","templateId":2,"failTemplateId":3,
       "recipientEmail":"fl7@ubs.com","mergeFields":{"fieldLeaderName":"Pat Leader"},
       "scheduledAt":"2026-07-01T08:00:00Z"}'

# Inspect outcomes (merged subject/body are stored for reference)
curl 'http://localhost:8080/api/v1/emails?status=FAILED'
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
| `STAATEMAIL_TENANT_ID`, `STAATEMAIL_CLIENT_ID`, `STAATEMAIL_CLIENT_SECRET` | dedicated StaatEmail SPN — OAuth2 client-credentials flow (MSAL). Omit to fall back to the app's shared credential (local dev). |
| `STAATEMAIL_SENDER_GPN`, `STAATEMAIL_FROM_GPN` | sender identity stamped on every `/sendEmail` |
| `STAATEMAIL_FROM_ADDRESS`, `STAATEMAIL_SENDER_ADDRESS`, `STAATEMAIL_REPLY_TO` | sender/reply addresses |
| `SCHEDULER_POLL_INTERVAL` | ISO-8601 batch/retry poll interval (`PT1H` hourly, `P1D` daily) |
| `EMAIL_MAX_ATTACHMENT_BYTES`, `EMAIL_RETRY_WINDOW` | per-email size cap (35 MB default) and retry window (`P7D`) |

The DB access token is requested for scope `https://ossrdbms-aad.database.windows.net/.default`
and used as the PostgreSQL password via the driver's native dynamic-password support
(see `R2dbcPasswordlessConfig` — the Microsoft token-as-password pattern; Spring Cloud Azure's
passwordless starter covers JDBC only, this is the R2DBC equivalent). StaatEmail calls get a
Bearer token for `STAATEMAIL_SCOPE` via `EntraBearerExchangeFilter`.

> The application **never executes SQL/DDL scripts** (`spring.sql.init.mode=never`). The schema is
> managed externally (DBA / deployment pipeline / migration tooling). `db/schema.sql` and
> `db/seed.sql` live in `src/test/resources` and are applied only to the embedded test database.
> The service role needs USAGE + DML on `aat_app` and USAGE + SELECT-only on `datamesh`.

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
