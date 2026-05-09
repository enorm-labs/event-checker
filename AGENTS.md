# AGENTS.md

## Project Overview

Event Checker is a multi-module Kotlin/Spring Boot application for discovering music events in Berlin. It uses a **Gradle multi-project build** with three
subprojects sharing a root `settings.gradle.kts`, plus a standalone frontend project:

- **`events-core`** – Shared domain model library (no Boot app); consumed via `project(":events-core")` dependency. Applies `java-library`, `maven-publish`,
  and `java-test-fixtures` plugins (add fixtures under `src/testFixtures/`). Uses `api()` scope for `spring-modulith-starter-core` so it's transitively
  available to consumers. Contains domain data classes organized by feature: `artist/`, `event/`, `promoter/`, `venue/`. Also defines enums (`EventType`,
  `EventStatus`, `ArtistRole`) and the `EventArtist` join entity in `event/Event.kt`.
- **`events-bff`** – Backend-for-Frontend REST API (Spring Boot 4 + WebFlux + R2DBC). Runs on default port `8080`.
- **`events-importer`** – Imports events from external sources into the database (Spring Boot 4 + WebFlux + R2DBC + Flyway). Runs on port `8081`.
  Owns all Flyway migrations under `src/main/resources/db/migration/`.
- **`events-frontend`** – Vue 3 SPA (Vite 8, TypeScript 6, Pinia, Vue Router). Uses oxlint/oxfmt for linting/formatting. Not a Gradle subproject — managed
  separately via npm. Requires Node `^20.19.0 || >=22.12.0` (see `engines` in `package.json`).

## Architecture Decisions

- **Reactive stack throughout**: WebFlux + R2DBC + Kotlin coroutines. Do NOT use blocking APIs (`spring-web`, JDBC) in request paths.
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
- **SpringDoc OpenAPI** enabled in both BFF and importer — Swagger UI available at `/swagger-ui.html` by default.
- **Jackson 3.x** (`tools.jackson.module:jackson-module-kotlin`) is used for JSON serialization.
- **Spring Boot Actuator** is included in both BFF and importer for health checks and monitoring.

## Build & Dev Commands

```bash
./gradlew clean build          # Full build (all modules, tests, ktlint)
./gradlew :events-bff:bootRun  # Run BFF (auto-starts Postgres via compose.yaml)
./gradlew :events-importer:bootRun  # Run importer
./gradlew ktlintCheck          # Lint all modules
./gradlew ktlintFormat         # Auto-fix formatting
./gradlew dependencyUpdates    # Check for newer dependency versions
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
- Centralized versions in root `build.gradle.kts` (`extra["java.version"]`, `extra["spring-modulith.version"]`, `extra["springdoc.version"]`); plugin versions
  in `settings.gradle.kts` `pluginManagement`.
- Use `val` for injected dependencies; constructor injection only (no field injection).
- Application config files use **`.yaml`** extension (not `.yml`).
- Kotlin compiler flags: `-Xjsr305=strict` (all modules) and `-Xannotation-default-target=param-property` (BFF + importer) are set in `compilerOptions`.
- **Kover** (`org.jetbrains.kotlinx.kover`) is available for code coverage reports.

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
- `ModularityTests` in each module (core, BFF, importer) validates Spring Modulith structure and generates docs to `build/spring-modulith-docs/`.
- `events-core` publishes test fixtures via `java-test-fixtures` plugin — consume with `testImplementation(testFixtures(project(":events-core")))`.

## CI/CD & Automation

- **GitHub Actions** runs three workflows (`.github/workflows/`):
    - `build-backend.yml` — Lint (`ktlintCheck`), build, and test all Gradle modules. Triggers on `main` push/PR, skips `events-frontend/**`, `*.md`, `docs/**`.
    - `build-frontend.yml` — Install, lint, build, unit test, and Playwright e2e test. Triggers only when `events-frontend/**` changes. Uses Node 24.
    - `dependency-submission.yml` — Submits Gradle dependency graph to GitHub on `main` push (for Dependabot alerts/security).
- **Dependabot** (`.github/dependabot.yml`) checks for Gradle dependency updates weekly.
- **Conventional Commits** — Commit messages follow the [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) spec.
  A reusable prompt is available at `.github/prompts/commit-message.prompt.md`.

## Key Files

| Purpose                             | Path                                                                              |
|-------------------------------------|-----------------------------------------------------------------------------------|
| Root build config & shared versions | `build.gradle.kts`                                                                |
| Plugin versions & module includes   | `settings.gradle.kts`                                                             |
| Dev database (Postgres)             | `compose.yaml`                                                                    |
| CI: backend build & test            | `.github/workflows/build-backend.yml`                                             |
| CI: frontend build & test           | `.github/workflows/build-frontend.yml`                                            |
| CI: dependency graph submission     | `.github/workflows/dependency-submission.yml`                                     |
| Dependabot config                   | `.github/dependabot.yml`                                                          |
| Commit message prompt               | `.github/prompts/commit-message.prompt.md`                                        |
| Shared domain module marker         | `events-core/src/.../EventsCoreModule.kt`                                         |
| Domain data classes                 | `events-core/src/.../artist/`, `event/`, `promoter/`, `venue/`                    |
| Initial DB migration                | `events-importer/src/main/resources/db/migration/V001__create_initial_schema.sql` |
| Testcontainers setup (BFF)          | `events-bff/src/test/.../PostgresTestcontainersConfiguration.kt`                  |
| Testcontainers setup (importer)     | `events-importer/src/test/.../PostgresTestcontainersConfiguration.kt`             |
| Modularity verification (BFF)       | `events-bff/src/test/.../ModularityTests.kt`                                      |
| Modularity verification (importer)  | `events-importer/src/test/.../ModularityTests.kt`                                 |
| Modularity verification (core)      | `events-core/src/test/.../ModularityTests.kt`                                     |
| Frontend entry point                | `events-frontend/src/main.ts`                                                     |
