# AGENTS.md

## Project Overview

Event Checker is a multi-module Kotlin/Spring Boot application for discovering music events in Berlin. It uses a **Gradle multi-project build** with three
subprojects sharing a root `settings.gradle.kts`, plus a standalone frontend project:

- **`events-core`** – Shared domain model library (no Boot app); consumed via `project(":events-core")` dependency. Applies `java-library`, `maven-publish`,
  and `java-test-fixtures` plugins (add fixtures under `src/testFixtures/`). Uses `api()` scope for `spring-modulith-starter-core` so it's transitively
  available to consumers. Contains domain data classes organized by feature: `artist/`, `event/`, `promoter/`, `venue/`. Also defines enums (`EventType`,
  `EventStatus`, `ArtistRole`) and the `LineupEntry` value object in `event/Event.kt`.
- **`events-bff`** – Backend-for-Frontend REST API (Spring Boot 4 + WebFlux + R2DBC). Runs on default port `8080`.
- **`events-importer`** – Imports events from external sources into the database (Spring Boot 4 + WebFlux + R2DBC + Flyway). Runs on port `8081`.
  Owns all Flyway migrations under `src/main/resources/db/migration/`.
- **`events-frontend`** – Vue 3 SPA (Vite 8, TypeScript 6, Pinia, Vue Router). Uses oxlint/oxfmt for linting/formatting. Not a Gradle subproject — managed
  separately via npm. Requires Node `^20.19.0 || >=22.12.0` (see `engines` in `package.json`).

## Architecture Decisions

- **Reactive stack throughout**: WebFlux + R2DBC + Kotlin coroutines. Do NOT use blocking APIs (`spring-web`, JDBC) in request paths.
  Repositories extend `CoroutineCrudRepository` so all operations are suspending functions.
- **Spring Data R2DBC query derivation limitations**: Unlike Spring Data JPA, R2DBC has more limited query derivation.
  Derived `findBy*`, `countBy*`, `existsBy*`, and `deleteBy*` methods are supported. However, derived `updateBy*` methods
  are **not supported** — use `@Modifying` + `@Query` with raw SQL instead. Custom `@Query` SQL must include the schema
  prefix (e.g. `events.event_artist`) because raw queries bypass the `@Table(schema = ...)` metadata.
  See [Spring Data R2DBC query methods reference](https://docs.spring.io/spring-data/relational/reference/r2dbc/query-methods.html).
- **Domain model** lives in `events-core` as plain Kotlin data classes (no Spring Data annotations). Tables: `venue`, `artist`, `promoter`, `event`,
  `event_artist` (join), `event_promoter` (join). Events reference venues via FK; artists and promoters link to events through join tables.
  `Event.sourceId` enables idempotent imports (upsert semantics).
- **Spring Modulith** enforces module boundaries: each direct sub-package under `de.norm.events` is an application module. Run `ModularityTests` (present in all
  three modules) to verify structure.
- **Database schema**: All tables live in a dedicated `events` schema (not `public`). Both apps configure this via `spring.r2dbc.properties.schema: events`;
  importer also sets `spring.flyway.schemas: events`. The migration `V001__create_initial_schema.sql` creates the schema with
  `CREATE SCHEMA IF NOT EXISTS events`.
- **Database migrations** live in `events-importer` only (Flyway). The BFF does not run migrations. Migration naming: `V001__description.sql`.
- **Docker Compose dev services**: `bootRun` auto-starts PostgreSQL via Spring Docker Compose support (`compose.yaml` at root).
- **SpringDoc OpenAPI** enabled in both BFF and importer — Swagger UI available at `/webjars/swagger-ui/index.html`;
  OpenAPI spec (JSON) at `/v3/api-docs`. Controllers are annotated with `@Tag(name = "Admin: <Entity>")` to group endpoints by entity type
  in Swagger UI (e.g. `"Admin: Venues"`, `"Admin: Events"`). Request and response DTOs use `@Schema` annotations on every field to provide
  descriptions, examples, and required-mode metadata in the generated API docs. Domain classes in `events-core` are intentionally kept free
  of Swagger annotations to avoid coupling the shared library to web concerns.
- **Jackson 3.x** (`tools.jackson.module:jackson-module-kotlin`) is used for JSON serialization.
- **Spring Boot Actuator** is included in both BFF and importer for health checks and monitoring.
- **Logging**: The importer uses [kotlin-logging](https://github.com/oshai/kotlin-logging) (`io.github.oshai:kotlin-logging-jvm`) as an idiomatic SLF4J
  wrapper. Declare loggers as: `private val logger = KotlinLogging.logger {}`. Use lambda syntax for lazy evaluation: `logger.info { "msg $var" }`.
- **Error handling**: The importer has a `GlobalExceptionHandler` (`@RestControllerAdvice`) that translates domain exceptions into
  RFC 9457 Problem Details (`ProblemDetail`). Domain exceptions follow the `*NotFoundException` naming pattern (e.g. `VenueNotFoundException`)
  and map to 404. `DataIntegrityViolationException` maps to 409 CONFLICT for duplicate records. `WebExchangeBindException` maps to 400
  BAD REQUEST for Bean Validation failures, with field-level error details in the response body. `IllegalArgumentException` maps to 500
  for data inconsistencies like unknown enum values from manual DB edits.
- **Request validation**: All `@RequestBody` parameters in controllers are annotated with `@Valid` to trigger Bean Validation.
  Request DTOs use `@field:NotBlank` on required string fields (e.g. `name`, `title`, `sourceId`), `@field:NotNull` on required
  non-string fields (e.g. `eventDate`), and `@field:Size` for max-length constraints. Nested DTOs use `@field:Valid` for cascading validation (e.g.
  `EventRequest.artists`).
  The `spring-boot-starter-validation` dependency provides the Jakarta Bean Validation API.
- **Entity/Domain separation**: Persistence entities (`*Entity.kt`) in the importer are separate from domain data classes in `events-core`.
  Entities carry Spring Data annotations (`@Table`, `@Id`) and provide `toDomain()` instance method + `fromDomain()` companion factory for conversion.
- **Request/Response DTOs**: Controllers accept `*Request` data classes and always return `*Response` DTOs — never domain objects directly.
  This decouples the API contract from the domain model so internal changes don't break the API. Each response DTO has a `companion object`
  with a `fromDomain()` factory method for conversion; services call `*Response.fromDomain()` and return the response DTO directly.
  **Exception**: `EventResponse` uses `fromEntity()` instead of `fromDomain()` because the event aggregate has complex associations
  (artists, promoters) that are resolved at the entity level rather than round-tripping through the domain model.
  The request → response flow is: `*Request` → Service (builds domain object → persists via `*Entity`) → `*Response.fromDomain()` → Controller.
  The event module uses plural filenames (`EventEntities.kt`, `EventRepositories.kt`, `EventRequests.kt`, `EventResponses.kt`) when a file
  contains multiple related classes; other modules use singular names (`VenueRequest.kt`, `VenueResponse.kt`, etc.).
- **Pagination, sorting & limiting**: All list endpoints accept `Pageable` parameters via query string (`?page=0&size=20&sort=name,asc`).
  Controllers use `@PageableDefault` to declare sensible defaults (20 items per page; venues/artists/promoters sort by `name`,
  events sort by `eventDate`). Repositories expose `findAllBy(pageable: Pageable): Flow<Entity>` for Spring Data to apply
  `LIMIT`/`OFFSET`/`ORDER BY`. `WebFluxConfiguration` registers `ReactivePageableHandlerMethodArgumentResolver` so WebFlux
  can resolve `Pageable` from request parameters (not auto-configured unlike Spring MVC).
  The event list endpoint uses batch loading to avoid N+1 queries: it fetches the current page of events, then bulk-fetches
  all artist and promoter associations for that page in 2 additional queries (3 queries total per page).
- **API path convention**: All importer admin endpoints live under `/api/admin/<resource>` (e.g. `/api/admin/venues`, `/api/admin/events`).
- **Module metadata**: Each feature package in the importer has a `*Module.kt` marker class annotated with
  `@ApplicationModule(allowedDependencies = [...])` to declare allowed inter-module dependencies for Spring Modulith verification.
  Similarly, `events-core` has `*Module.kt` markers per feature package (`ArtistModule`, `EventModule`, `PromoterModule`, `VenueModule`)
  plus a root `EventsCoreModule.kt`.
- **Importer feature module structure**: Each feature package follows a consistent file layout:
  `*Controller.kt`, `*Service.kt`, `*Repository.kt`, `*Entity.kt`, `*Request.kt`, `*Response.kt`, `*Module.kt`, `*NotFoundException.kt`.
- **Slugify**: The importer uses `com.github.slugify:slugify` to always auto-generate URL-friendly slugs from entity names.
  Slugs are not accepted in request DTOs — they are a server-side concern computed by the service layer.
  The slug logic is encapsulated in a dedicated `slug` Spring Modulith module (`de.norm.events.slug`) with a `SlugGenerator`
  object singleton (see `SlugGenerator.kt`, `SlugModule.kt`). All feature modules declare `"slug"` in their `allowedDependencies`.
- **OWASP Dependency-Check** (`org.owasp.dependencycheck`) scans all project dependencies against the National Vulnerability
  Database (NVD) for known CVEs. Configured at the root level with `dependencyCheckAggregate` to produce a single report.
  Fails the build on CVSS ≥ 7 (HIGH). False positives can be suppressed via `owasp-suppressions.xml`. SARIF output is
  uploaded to GitHub Code Scanning; the HTML report is uploaded as a CI artifact.

## Build & Dev Commands

```bash
./gradlew clean build          # Full build (all modules, tests, ktlint)
./gradlew :events-bff:bootRun  # Run BFF (auto-starts Postgres via compose.yaml)
./gradlew :events-importer:bootRun  # Run importer
./gradlew ktlintCheck          # Lint all modules
./gradlew ktlintFormat         # Auto-fix formatting
./gradlew detekt               # Static analysis (all modules)
./gradlew koverLog             # Print test coverage summary per module
./gradlew koverHtmlReport      # Generate HTML coverage reports
./gradlew dependencyUpdates    # Check for newer dependency versions
./gradlew dependencyCheckAggregate  # OWASP Dependency-Check (CVE scan)
./gradlew httpTest                  # Run .http files via IntelliJ HTTP Client CLI (requires ijhttp + running importer)
```

Frontend (`events-frontend/`):

```bash
npm run dev        # Vite dev server
npm run build      # Type-check + production build
npm run test:unit  # Vitest unit tests
npm run test:e2e   # Playwright end-to-end tests
npm run lint       # oxlint + eslint (auto-fix)
npm run format     # oxfmt formatter
```

Java version is managed via SDKMAN (`.sdkmanrc` pins `java=25.0.2-tem`; run `sdk env` to activate). Toolchain target: **Java 25**.

## Code Conventions

- **Package structure**: `de.norm.events.<module-name>` — organize by feature/domain, not layer.
- **Kotlin DSL** for all Gradle build scripts (`build.gradle.kts`).
- **Kotlin 2.3.21** with **Spring Boot 4.0.6**; plugin versions pinned in `settings.gradle.kts` `pluginManagement`.
- **ktlint 1.8.0** enforced project-wide via root `subprojects` block; do not override per-module.
- **detekt 2.0** (`dev.detekt` plugin, migrated from `io.gitlab.arturbosch.detekt`) applied project-wide. Builds upon default config with overrides in
  root `detekt.yml` (currently only `MaxLineLength: 160`). Run `./gradlew detekt` to analyze all modules.
- **Max line length**: 160 characters (enforced by both `.editorconfig` and `detekt.yml`).
- Centralized versions in root `build.gradle.kts` (`extra["java.version"]`, `extra["kotest.version"]`, `extra["kotlin-logging.version"]`,
  `extra["slugify.version"]`, `extra["spring-modulith.version"]`, `extra["springdoc.version"]`); plugin versions in `settings.gradle.kts` `pluginManagement`.
- Use `val` for injected dependencies; constructor injection only (no field injection).
- Application config files use **`.yaml`** extension (not `.yml`).
- Kotlin compiler flags: `-Xjsr305=strict` (all modules) and `-Xannotation-default-target=param-property` (BFF + importer) are set in `compilerOptions`.
- **Kover** (`org.jetbrains.kotlinx.kover`) is configured for code coverage reports. Run `./gradlew koverLog` for a console summary
  or `./gradlew koverHtmlReport` for detailed HTML reports.

## Testing Patterns

- **JUnit 5** + **WebTestClient** for reactive endpoint tests (see `HelloControllerTest.kt`).
  Create the client via lazy delegate with `@LocalServerPort`:
  ```kotlin
  @LocalServerPort private var port: Int = 0
  private val webTestClient: WebTestClient by lazy {
      WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
  }
  ```
- **Spring Boot 4 test starters**: Each runtime starter has a `*-test` companion (e.g. `spring-boot-starter-webflux-test`,
  `spring-boot-starter-data-r2dbc-test`). Always add the `-test` variant alongside the main starter.
- Tests requiring PostgreSQL import `PostgresTestcontainersConfiguration` via `@Import` — this provides a reusable Testcontainers `@ServiceConnection` bean.
  Both `events-bff` and `events-importer` have their own copy.
- Testcontainers use `PostgreSQLContainer("postgres:18.3-alpine").withReuse(true)` to match the dev compose image and speed up repeated test runs.
  Uses modular Testcontainers 2.x artifacts (`org.testcontainers:testcontainers-postgresql`, `testcontainers-r2dbc`, `testcontainers-junit-jupiter`)
  with modular package imports (`org.testcontainers.postgresql.PostgreSQLContainer`).
- Use backtick function names for readable test descriptions: `` `GET hello returns Hello world`() ``.
- **BaseControllerTest** (importer only): Abstract base class for integration tests that extends Testcontainers setup, provides a `WebTestClient`,
  and truncates all domain tables via `@BeforeEach` so each test starts with a clean database. Extend this instead of repeating boilerplate.
- **Kotest assertions**: The importer uses `io.kotest:kotest-assertions-core` for expressive test matchers (e.g. `shouldBe`, `shouldContain`).
- **Test fixture factories**: Each importer feature module has a `*RequestFixtures` object singleton with factory methods that provide sensible defaults,
  so tests only override properties relevant to the scenario (e.g. `VenueRequestFixtures.astra()`, `VenueRequestFixtures.create(name = "Privatclub")`).
- **Full lifecycle integration test**: `FullLifecycleIntegrationTest` exercises the complete CRUD flow across all entity types in a single sequential scenario
  (create → list → get → update → delete), mirroring the `full-lifecycle.http` script. Extend this pattern for new cross-entity workflows.
- `ModularityTests` in each module (core, BFF, importer) validates Spring Modulith structure and generates docs to `build/spring-modulith-docs/`.
- `events-core` publishes test fixtures via `java-test-fixtures` plugin — consume with `testImplementation(testFixtures(project(":events-core")))`.

## CI/CD & Automation

- **GitHub Actions** runs four workflows (`.github/workflows/`):
    - `build-backend.yml` — Lint (`ktlintCheck`), static analysis (`detekt`), build, test, and OWASP dependency CVE scan.
      Posts detekt markdown reports and Kover coverage to the job summary; on PRs, also posts Kover coverage as a sticky comment
      (via `mi-kas/kover-report`). Detekt SARIF reports are uploaded per module to GitHub Code Scanning. Triggers on `main` push/PR, skips
      `events-frontend/**`, `*.md`, `docs/**`.
    - `build-frontend.yml` — Install, lint, build, unit test, and Playwright e2e test. Triggers only when `events-frontend/**` changes. Uses Node 24.
    - `dependency-review.yml` — Runs on PRs to diff dependency changes between base and head. Flags newly introduced vulnerabilities
      (high+ severity) and license issues using the GitHub Advisory Database. Complements OWASP Dependency-Check with fast, PR-scoped feedback.
    - `dependency-submission.yml` — Submits Gradle dependency graph to GitHub on `main` push (for Dependabot alerts/security).
- **Dependabot** (`.github/dependabot.yml`) checks for Gradle dependency updates weekly. Updates are grouped by ecosystem
  (e.g. `kotlin`, `spring-boot`, `spring-modulith`, `testcontainers`, `jackson`, `springdoc`, `kotest`, `postgresql`, `flyway`, `reactor`, `detekt`, `owasp`,
  `gradle-plugins`)
  so that related dependencies are bundled into a single PR per group.
- **Conventional Commits** — Commit messages follow the [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) spec.
  Reusable prompts are available at `.github/prompts/` for commit messages, squash commit messages, and code reviews.

## Key Files

| Purpose                               | Path                                                                              |
|---------------------------------------|-----------------------------------------------------------------------------------|
| Root build config & shared versions   | `build.gradle.kts`                                                                |
| Plugin versions & module includes     | `settings.gradle.kts`                                                             |
| Gradle daemon JVM args                | `gradle.properties`                                                               |
| Dev database (Postgres)               | `compose.yaml`                                                                    |
| Detekt rule overrides                 | `detekt.yml`                                                                      |
| OWASP CVE false-positive suppressions | `owasp-suppressions.xml`                                                          |
| CI: backend build & test              | `.github/workflows/build-backend.yml`                                             |
| CI: frontend build & test             | `.github/workflows/build-frontend.yml`                                            |
| CI: dependency review (PR)            | `.github/workflows/dependency-review.yml`                                         |
| CI: dependency graph submission       | `.github/workflows/dependency-submission.yml`                                     |
| Dependabot config                     | `.github/dependabot.yml`                                                          |
| Commit message prompt                 | `.github/prompts/commit-message.prompt.md`                                        |
| Squash commit message prompt          | `.github/prompts/squash-commit-message.prompt.md`                                 |
| Code review prompt                    | `.github/prompts/code-review.prompt.md`                                           |
| Shared domain module marker           | `events-core/src/.../EventsCoreModule.kt`                                         |
| Domain data classes                   | `events-core/src/.../artist/`, `event/`, `promoter/`, `venue/`                    |
| Initial DB migration                  | `events-importer/src/main/resources/db/migration/V001__create_initial_schema.sql` |
| Global exception handler              | `events-importer/src/.../GlobalExceptionHandler.kt`                               |
| Slug generator utility                | `events-importer/src/.../slug/SlugGenerator.kt`                                   |
| WebFlux Pageable resolver config      | `events-importer/src/.../WebFluxConfiguration.kt`                                 |
| Base integration test class           | `events-importer/src/test/.../BaseControllerTest.kt`                              |
| Full lifecycle integration test       | `events-importer/src/test/.../event/FullLifecycleIntegrationTest.kt`              |
| Testcontainers setup (BFF)            | `events-bff/src/test/.../PostgresTestcontainersConfiguration.kt`                  |
| Testcontainers setup (importer)       | `events-importer/src/test/.../PostgresTestcontainersConfiguration.kt`             |
| Modularity verification (BFF)         | `events-bff/src/test/.../ModularityTests.kt`                                      |
| Modularity verification (importer)    | `events-importer/src/test/.../ModularityTests.kt`                                 |
| Modularity verification (core)        | `events-core/src/test/.../ModularityTests.kt`                                     |
| ADR: Reactive stack                   | `docs/adr/ADR-001_REACTIVE_STACK.md`                                              |
| ADR: R2DBC query derivation limits    | `docs/adr/ADR-002_R2DBC_QUERY_DERIVATION.md`                                      |
| ADR: Entity/domain separation         | `docs/adr/ADR-003_ENTITY_DOMAIN_SEPARATION.md`                                    |
| ADR: Dedicated database schema        | `docs/adr/ADR-004_DEDICATED_DATABASE_SCHEMA.md`                                   |
| ADR: Migrations owned by importer     | `docs/adr/ADR-005_MIGRATIONS_OWNED_BY_IMPORTER.md`                                |
| ADR: Spring Modulith                  | `docs/adr/ADR-006_SPRING_MODULITH.md`                                             |
| Frontend entry point                  | `events-frontend/src/main.ts`                                                     |
| IntelliJ HTTP Client requests         | `http/` (per-entity `.http` files + `http-client.env.json` for environments)      |
