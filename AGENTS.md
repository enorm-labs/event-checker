# AGENTS.md

## Project Overview

Event Checker is a multi-module Kotlin/Spring Boot application for discovering music events in Berlin. It uses a **Gradle multi-project build** with three
subprojects sharing a root `settings.gradle.kts`:

- **`events-core`** – Shared domain model library (no Boot app); consumed via `project(":events-core")` dependency. Publishes `java-test-fixtures` for shared
  test utilities.
- **`events-bff`** – Backend-for-Frontend REST API (Spring Boot 4 + WebFlux + R2DBC). Runs on default port `8080`.
- **`events-importer`** – Imports events from external sources into the database (Spring Boot 4 + WebFlux + R2DBC + Flyway). Runs on port `8081`.
- **`events-frontend`** – Vue 3 SPA (Vite 8, TypeScript 6, Pinia, Vue Router). Uses oxlint/oxfmt for linting/formatting.

## Architecture Decisions

- **Reactive stack throughout**: WebFlux + R2DBC + Kotlin coroutines. Do NOT use blocking APIs (`spring-web`, JDBC) in request paths.
- **Spring Modulith** enforces module boundaries: each direct sub-package under `de.norm.events` is an application module. Run `ModularityTests` (present in all
  three modules) to verify structure.
- **Database migrations** live in `events-importer` only (Flyway). The BFF does not run migrations.
- **Docker Compose dev services**: `bootRun` auto-starts PostgreSQL via Spring Docker Compose support (`compose.yaml` at root).
- **SpringDoc OpenAPI** enabled in both BFF and importer — Swagger UI available at `/swagger-ui.html` by default.
- **Jackson 3.x** (`tools.jackson.module:jackson-module-kotlin`) is used for JSON serialization.

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

Java version is managed via SDKMAN (run `sdk env` to activate). Toolchain target: **Java 25**.

## Code Conventions

- **Package structure**: `de.norm.events.<module-name>` — organize by feature/domain, not layer.
- **Kotlin DSL** for all Gradle build scripts (`build.gradle.kts`).
- **ktlint 1.8.0** enforced project-wide via root `subprojects` block; do not override per-module.
- Centralized versions in root `build.gradle.kts` (`extra["java.version"]`, `extra["spring-modulith.version"]`, `extra["springdoc.version"]`); plugin versions
  in `settings.gradle.kts` `pluginManagement`.
- Use `val` for injected dependencies; constructor injection only (no field injection).
- Application config files use **`.yaml`** extension (not `.yml`).

## Testing Patterns

- **JUnit 5** + **WebTestClient** for reactive endpoint tests (see `HelloControllerTest.kt`).
- Tests requiring PostgreSQL import `PostgresTestcontainersConfiguration` via `@Import` — this provides a reusable Testcontainers `@ServiceConnection` bean.
  Both `events-bff` and `events-importer` have their own copy.
- Testcontainers use `PostgreSQLContainer("postgres:18.3-alpine").withReuse(true)` to match the dev compose image and speed up repeated test runs.
- Use backtick function names for readable test descriptions: `` `GET hello returns Hello world`() ``.
- `ModularityTests` in each module (core, BFF, importer) validates Spring Modulith structure and generates docs to `build/spring-modulith-docs/`.
- `events-core` publishes test fixtures via `java-test-fixtures` plugin — consume with `testImplementation(testFixtures(project(":events-core")))`.

## Key Files

| Purpose                             | Path                                                                  |
|-------------------------------------|-----------------------------------------------------------------------|
| Root build config & shared versions | `build.gradle.kts`                                                    |
| Plugin versions & module includes   | `settings.gradle.kts`                                                 |
| Dev database (Postgres)             | `compose.yaml`                                                        |
| Shared domain module marker         | `events-core/src/.../EventsCoreModule.kt`                             |
| Testcontainers setup (BFF)          | `events-bff/src/test/.../PostgresTestcontainersConfiguration.kt`      |
| Testcontainers setup (importer)     | `events-importer/src/test/.../PostgresTestcontainersConfiguration.kt` |
| Modularity verification (BFF)       | `events-bff/src/test/.../ModularityTests.kt`                          |
| Modularity verification (importer)  | `events-importer/src/test/.../ModularityTests.kt`                     |
| Modularity verification (core)      | `events-core/src/test/.../ModularityTests.kt`                         |
| Frontend entry point                | `events-frontend/src/main.ts`                                         |
