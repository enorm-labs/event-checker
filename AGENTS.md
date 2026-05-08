# AGENTS.md

## Project Overview

Event Checker is a multi-module Kotlin/Spring Boot application for discovering music events in Berlin. It uses a **Gradle multi-project build** with three subprojects sharing a root `settings.gradle.kts`:

- **`events-core`** – Shared domain model library (no Boot app); consumed via `project(":events-core")` dependency.
- **`events-bff`** – Backend-for-Frontend REST API (Spring Boot + WebFlux + R2DBC).
- **`events-importer`** – Imports events from external sources into the database (Spring Boot + WebFlux + R2DBC + Flyway).
- **`events-frontend`** – Vue 3 SPA (separate Node/npm toolchain).

## Architecture Decisions

- **Reactive stack throughout**: WebFlux + R2DBC + Kotlin coroutines. Do NOT use blocking APIs (`spring-web`, JDBC) in request paths.
- **Spring Modulith** enforces module boundaries: each direct sub-package under `de.norm.events` is an application module. Run `ModularityTests` to verify structure.
- **Database migrations** live in `events-importer` only (Flyway). The BFF does not run migrations.
- **Docker Compose dev services**: `bootRun` auto-starts PostgreSQL via Spring Docker Compose support (`compose.yaml` at root).

## Build & Dev Commands

```bash
./gradlew clean build          # Full build (all modules, tests, ktlint)
./gradlew :events-bff:bootRun  # Run BFF (auto-starts Postgres via compose.yaml)
./gradlew :events-importer:bootRun  # Run importer
./gradlew ktlintCheck          # Lint all modules
./gradlew ktlintFormat         # Auto-fix formatting
./gradlew dependencyUpdates    # Check for newer dependency versions
```

Java version is managed via SDKMAN (run `sdk env` to activate). Toolchain target: **Java 25**.

## Code Conventions

- **Package structure**: `de.norm.events.<module-name>` — organize by feature/domain, not layer.
- **Kotlin DSL** for all Gradle build scripts (`build.gradle.kts`).
- **ktlint 1.8.0** enforced project-wide via root `subprojects` block; do not override per-module.
- Centralized versions in root `build.gradle.kts` (`extra["java.version"]`, `extra["spring-modulith.version"]`); plugin versions in `settings.gradle.kts` `pluginManagement`.
- Use `val` for injected dependencies; constructor injection only (no field injection).

## Testing Patterns

- **JUnit 5** + **WebTestClient** for reactive endpoint tests (see `HelloControllerTest.kt`).
- Tests requiring PostgreSQL import `PostgresTestcontainersConfiguration` via `@Import` — this provides a reusable Testcontainers `@ServiceConnection` bean.
- Use backtick function names for readable test descriptions: `` `GET hello returns Hello world`() ``.
- `ModularityTests` in each Boot module validates Spring Modulith structure and generates docs to `build/spring-modulith-docs/`.

## Key Files

| Purpose | Path |
|---------|------|
| Root build config & shared versions | `build.gradle.kts` |
| Plugin versions & module includes | `settings.gradle.kts` |
| Dev database (Postgres) | `compose.yaml` |
| Shared domain module marker | `events-core/src/.../EventsCoreModule.kt` |
| Testcontainers setup (reused) | `events-bff/src/test/.../PostgresTestcontainersConfiguration.kt` |
| Modularity verification | `events-bff/src/test/.../ModularityTests.kt` |

