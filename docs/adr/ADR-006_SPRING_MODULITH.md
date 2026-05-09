# ADR-006: Spring Modulith for Module Boundary Enforcement

## Status

Accepted

## Context

The application is organized by feature/domain (`venue`, `artist`, `event`, `promoter`). As the codebase
grows, it's easy for modules to develop unintended dependencies — e.g. the venue module accidentally importing
something from the event module, creating a circular or undesired coupling.

Two approaches were considered:

1. **Convention + code review** — Rely on developers to respect module boundaries. No tooling enforcement.
   Works for small teams but breaks down as the codebase scales.
2. **Spring Modulith** — Declare module boundaries with annotations and verify them automatically in tests.
   Catches violations at build time.

## Decision

Use **Spring Modulith** to enforce module boundaries across all three Gradle subprojects (`events-core`,
`events-bff`, `events-importer`).

Each direct sub-package under `de.norm.events` is an application module. Module metadata is declared via
a `*Module.kt` marker class:

```kotlin
@ApplicationModule(allowedDependencies = [])
class VenueModule  // Self-contained, no dependencies on other modules

@ApplicationModule(allowedDependencies = ["artist", "venue", "promoter"])
class EventModule  // Events depend on artists, venues, and promoters
```

`ModularityTests` in each subproject verify the declared structure:

```kotlin
class ModularityTests {
    @Test
    fun `verify modular structure`() {
        ApplicationModules.of(EventsImporterApplication::class.java).verify()
    }
}
```

This test fails the build if any module accesses another module's internals or uses an undeclared dependency.

## Consequences

- **Positive**: Module boundaries are enforced at build time, not just by convention; violations are caught
  immediately in CI; `allowedDependencies` serves as living documentation of the dependency graph; Spring
  Modulith can generate module documentation to `build/spring-modulith-docs/`.
- **Negative**: Every new feature module needs a `*Module.kt` marker and must declare its dependencies; adding
  a cross-module dependency requires updating `allowedDependencies` (this friction is intentional — it forces
  a conscious decision).
- The `spring-modulith-starter-core` dependency is declared with `api()` scope in `events-core` so it's
  transitively available to all consumers.

## References

- [Spring Modulith documentation](https://docs.spring.io/spring-modulith/reference/)
- [`VenueModule.kt`](../../events-importer/src/main/kotlin/de/norm/events/venue/VenueModule.kt) (example marker)
- [`ModularityTests.kt`](../../events-importer/src/test/kotlin/de/norm/events/ModularityTests.kt)

