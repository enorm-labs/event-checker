# AGENTS.md

## Agent Instructions

- **Git non-interactive mode**: Always run git commands with the pager disabled to prevent the agent from hanging
  on interactive output. Use `git --no-pager <command>` or set the environment variable `GIT_PAGER=cat`.
  This applies to all git commands that may produce paged output (`log`, `diff`, `show`, `branch`, etc.).
  See [git docs](https://git-scm.com/docs/git#Documentation/git.txt---no-pager).
- **ktlint auto-format first**: When ktlint reports formatting issues, always run `./gradlew ktlintFormat` first
  to auto-correct them. Only edit files manually for issues that ktlint cannot auto-fix.
- **Build verification**: Always run `./gradlew clean build` after finishing an implementation to verify that all modules
  compile, tests pass, ktlint and detekt checks succeed, and Kover coverage thresholds are met.
  **Skip this step** when only Markdown documentation (`.md` files) or frontend files (`events-frontend/`) were changed —
  the Gradle build covers the backend modules only.
- **No unsolicited git commits/pushes**: Never run `git commit`, `git push`, or `git rebase` (squash) unless explicitly asked to by the user.
- **GitHub CLI (`gh`)**: The `gh` CLI is installed (Homebrew) and authenticated for GitHub.com and enterprise instances.
  Use it for GitHub interactions such as creating/viewing PRs, managing issues, checking CI status, and browsing repositories.
  See [GitHub CLI quickstart](https://docs.github.com/en/github-cli/github-cli/quickstart) and
  [CLI reference](https://docs.github.com/en/github-cli/github-cli/github-cli-reference).

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
  `event_artist` (join), `event_promoter` (join), `genre_tag`, `event_genre_tag` (join), `event_source` (import metadata). Events reference venues via FK;
  artists, promoters, and genre tags link to events through join tables.
  `Event.sourceId` enables idempotent imports (upsert semantics). `event_source` tracks per-venue import configuration and conditional-request headers (ETag,
  Last-Modified).
- **Spring Modulith** enforces module boundaries: each direct sub-package under `de.norm.events` is an application module. Run `ModularityTests` (present in all
  three modules) to verify structure.
- **Database schema**: All tables live in a dedicated `events` schema (not `public`). Both apps configure this via `spring.r2dbc.properties.schema: events`;
  importer also sets `spring.flyway.schemas: events`, which tells Flyway to auto-create the schema and set `search_path` before running migrations.
  A custom `NamingStrategy` bean in `R2dbcConfiguration` reads the schema from `spring.r2dbc.properties.schema` and applies it
  globally to all derived query methods (`findBy*`, `save`, `delete`, etc.), so `@Table` annotations don't need to repeat
  `schema = "events"`. Without this `NamingStrategy`, Spring Data R2DBC generates unqualified table references
  (e.g. `INSERT INTO "venue"`) that fail because the tables don't exist in the `public` schema. Raw `@Query` SQL must
  still include the schema prefix manually (e.g. `events.event_source`) since custom queries bypass both `@Table` and
  `NamingStrategy` metadata.
- **Database migrations** live in `events-importer` only (Flyway). The BFF does not run migrations. Migration naming: `V001__description.sql`.
  While the project is in development (not yet deployed to production), all schema changes are consolidated into a single
  `V001__create_initial_schema.sql` migration. Incremental migrations (`V002`, `V003`, …) will be introduced once the first
  production deployment establishes a baseline.
- **Docker Compose dev services**: `bootRun` auto-starts PostgreSQL via Spring Docker Compose support (`compose.yaml` at root).
- **SpringDoc OpenAPI** enabled in both BFF and importer — Swagger UI available at `/webjars/swagger-ui/index.html`;
  OpenAPI spec (JSON) at `/v3/api-docs`. Controllers are annotated with `@Tag(name = "Admin: <Entity>")` to group endpoints by entity type
  in Swagger UI (e.g. `"Admin: Venues"`, `"Admin: Events"`). Request and response DTOs use `@Schema` annotations on every field to provide
  descriptions, examples, and required-mode metadata in the generated API docs. Domain classes in `events-core` are intentionally kept free
  of Swagger annotations to avoid coupling the shared library to web concerns.
- **Jackson 3.x** (`tools.jackson.module:jackson-module-kotlin`) is used for JSON serialization.
- **Spring Boot Actuator** is included in both BFF and importer for health checks and monitoring.
- **Logging**: Both apps use [kotlin-logging](https://github.com/oshai/kotlin-logging) (`io.github.oshai:kotlin-logging-jvm`) as an idiomatic SLF4J
  wrapper. Declare loggers as: `private val logger = KotlinLogging.logger {}`. Use lambda syntax for lazy evaluation: `logger.info { "msg $var" }`.
  The BFF registers a `RequestLoggingFilter` (`WebFilter`, `HIGHEST_PRECEDENCE`) that emits one INFO access-log line per request
  (`GET /venues?q=astra -> 200 (12ms)`); WebFlux does not log requests at INFO by default.
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
  contains multiple related classes; the scraper module also uses `EventSourceResponses.kt` for the same reason; the genre tag module uses
  `GenreTagEntities.kt` and `GenreTagRepositories.kt` for the same reason. Other modules use singular names
  (`VenueRequest.kt`, `VenueResponse.kt`, etc.).
- **Pagination, sorting & limiting**: All list endpoints accept `Pageable` parameters via query string (`?page=0&size=20&sort=name,asc`).
  Controllers use `@PageableDefault` to declare sensible defaults (20 items per page; venues/artists/promoters sort by `name`,
  events sort by `eventDate`). Repositories expose `findAllBy(pageable: Pageable): Flow<Entity>` for Spring Data to apply
  `LIMIT`/`OFFSET`/`ORDER BY`. `WebFluxConfiguration` registers `ReactivePageableHandlerMethodArgumentResolver` so WebFlux
  can resolve `Pageable` from request parameters (not auto-configured unlike Spring MVC).
  The event list endpoint uses batch loading to avoid N+1 queries: it fetches the current page of events, then bulk-fetches
  all artist, promoter, and genre tag associations for that page in 3 additional queries (4 queries total per page).
- **API path convention**: All importer admin endpoints live under `/api/admin/<resource>` (e.g. `/api/admin/venues`, `/api/admin/events`).
- **Module metadata**: Each feature package in the importer has a `*Module.kt` marker class annotated with
  `@ApplicationModule(allowedDependencies = [...])` to declare allowed inter-module dependencies for Spring Modulith verification.
  Similarly, `events-core` has `*Module.kt` markers per feature package (`ArtistModule`, `EventModule`, `GenreTagModule`, `PromoterModule`, `VenueModule`)
  plus a root `EventsCoreModule.kt`.
- **Importer feature module structure**: Each feature package follows a consistent file layout:
  `*Controller.kt`, `*Service.kt`, `*Repository.kt`, `*Entity.kt`, `*Request.kt`, `*Response.kt`, `*Module.kt`, `*NotFoundException.kt`.
- **Slugify**: The importer uses `com.github.slugify:slugify` to always auto-generate URL-friendly slugs from entity names.
  Slugs are not accepted in request DTOs — they are a server-side concern computed by the service layer.
  The slug logic is encapsulated in a dedicated `slug` Spring Modulith module (`de.norm.events.slug`) with a `SlugGenerator`
  object singleton (see `SlugGenerator.kt`, `SlugModule.kt`). All feature modules declare `"slug"` in their `allowedDependencies`.
- **Price normalization**: All monetary `BigDecimal` fields (presale, box office) are normalized to scale 2 (`setScale(2, HALF_UP)`)
  at the mapping boundaries where prices enter `EventEntity` — scraper (`ScrapedEvent.toEventEntity()`) and admin API (`EventService`).
  The `BigDecimal.normalizeMoneyScale()` extension function lives in `events-core` (`MoneyExtensions.kt`) as a domain-level concern.
  This ensures consistent storage and prevents false positives in the scraper's `contentEquals` change detection, because
  `BigDecimal.equals()` is scale-sensitive (e.g. `BigDecimal("10.0") != BigDecimal("10.00")`).
- **Genre tags**: The `genre` column on events stores raw free-text from venue websites for display. A separate `genre_tag`
  table and `event_genre_tag` join table provide normalized many-to-many genre tags for structured filtering. Genre tags are
  auto-created during event imports and admin API calls — there is no manual CRUD API. The `GenreNormalizer` utility in the
  `genretag` module parses raw genre strings by splitting on common delimiters (`,`, `//`, ` & `, ` / `), stripping noise
  suffixes ("Floor", "etc."), and mapping known synonyms to canonical names (e.g. "Hip-Hop"/"Rap" → "Hip Hop"). Unknown genres
  are kept with title case and auto-created as new tags. The normalizer is shared between the admin API (`EventService`) and
  the scraper pipeline (`AssociationSyncService`). The `GET /api/admin/genre-tags` endpoint provides the tag list for frontend
  filter dropdowns.
- **Web scraping**: The importer uses a `scraper` Spring Modulith module (`de.norm.events.scraper`) for importing event data
  from venue websites. See ADR-007. Key design:
    - **Jsoup** (`org.jsoup:jsoup`) for HTML parsing — robust handling of real-world HTML with CSS selector API.
    - **Spring WebClient** for reactive HTTP fetching with ETag/Last-Modified conditional requests.
    - **`PerHostThrottlingFilter`** — WebClient `ExchangeFilterFunction` enforcing a configurable
      politeness delay (`app.scraper.polite-delay-millis`, default: 200ms) between consecutive requests
      to the same host. Transparent to scrapers — all `HtmlFetcher` requests are throttled automatically.
      Requests to different hosts proceed concurrently. See ADR-007 "Per-Host Politeness Throttling".
    - **`EventSource` enum** — compile-time safe registry of known import sources (e.g. `CASSIOPEIA`).
    - **`EventImporter` interface** — each venue-specific importer implements this, dispatched by `eventSource` property.
    - **`event_source` table** — tracks per-venue import configuration, conditional-request headers, and import status/metrics.
      Event sources are created via `POST /api/admin/event-sources` (not Flyway — Flyway is reserved for DDL-only migrations).
      Operational config (enable/disable, interval, retries) is managed via `PATCH` and sources can be removed via `DELETE`.
    - **`EventImportService`** — orchestrates the import pipeline: resolves the correct importer, delegates persistence
      to `EventUpsertService`, and manages event source status transitions (RUNNING → SUCCESS/FAILED/MISCONFIGURED).
      Imports multiple sources concurrently using coroutine `async` with a `Semaphore`-based concurrency limit
      (`app.import.max-concurrency`, default: 4). This is safe because the artist cache is local to each import call,
      concurrent artist creation is handled via `DataIntegrityViolationException` fallback in `AssociationSyncService`,
      and per-host HTTP politeness is enforced by `PerHostThrottlingFilter`.
    - **`EventUpsertService`** — handles the event persistence pipeline: deduplication, change detection (skips unchanged
      events to avoid unnecessary writes and inflated `updated_at` timestamps), event upsert, and stale event cleanup.
      Delegates artist/promoter resolution and association syncing to `AssociationSyncService`.
      Called within a transactional boundary by `EventImportService`.
    - **`AssociationSyncService`** — resolves artists and promoters by slug (auto-creating unknown ones via
      `DataIntegrityViolationException` fallback for concurrent safety) and synchronizes many-to-many join-table
      associations using a diff strategy (insert new, update changed, delete stale). Called by `EventUpsertService`.
    - **`EventSourceService`** — CRUD service for managing event source configuration.
    - **Shared scraper utilities** — three focused extension files in the `scraper/` package, shared across all venue scrapers.
      New venue scrapers should use these utilities instead of reimplementing the same patterns.
        - **`ScrapingExtensions.kt`** — Jsoup HTML element extraction helpers (`Element.textAt()`, `Element.attrAt()`,
          `Element.imgSrcAt()`, `Element.hrefAt()`, `Element.hasVisibleWebflowFlag()`) and URL resolution (`resolveUrl()`).
        - **`DateParsingExtensions.kt`** — date/time parsing for the two common formats on venue websites: standalone
          `HH:mm` strings from HTML (`parseTime()`) and ISO 8601 datetime strings from schema.org JSON-LD (`parseIsoDate()`, `parseIsoTime()`).
        - **`EventMappingExtensions.kt`** — domain-level mapping of scraped text to model constants: German event category
          classification (`mapGermanCategory()`), placeholder name detection (`isPlaceholderName()`), and artist list
          construction from the headliner + support pattern (`buildArtistList()`).
    - **Venue-specific subdirectories** — each venue importer lives in its own sub-package under `scraper/` (e.g. `scraper/cassiopeia/`).
      Each contains a `*WebsiteImporter.kt` implementing `EventImporter`, plus `*OverviewPageScraper.kt` and `*DetailPageScraper.kt`
      for pure HTML parsing (no I/O). Use existing implementations as templates when adding new venue importers.
    - **`AbstractTwoPageWebsiteImporter`** — base class for venues that use the overview → detail pattern (currently
      Cassiopeia and Madame Claude). Owns the shared overview-fetch → per-event detail-fetch → gap-fill orchestration,
      including `NotModified` handling and the "degrade to overview data if the detail page fails" fallback. Subclasses
      implement only `scrapeOverview`, `scrapeDetail`, and `fillGapsFromOverview` (the venue-specific merge strategy).
      Single-page venues (e.g. `PrivatclubWebsiteImporter`) implement `EventImporter` directly instead.
      The two-layer strategy itself is the decision recorded in ADR-007; the abstract class is just the implementation vehicle.
- **Scheduled imports**: The importer uses Spring `@Scheduled` with the `event_source` table for periodic event imports. See ADR-008. Key design:
    - A single `@Scheduled(fixedDelay = 60s)` tick in `ScheduledImportService` queries for due sources.
    - Due sources are imported concurrently via `EventImportService.importConcurrently()`, bounded by the
      configured concurrency limit (`app.import.max-concurrency`, default: 4).
    - Each source has its own `import_interval_minutes` (default: 1440 = daily).
    - Failed imports are retried with exponential backoff up to `max_retries` times.
    - Sources stuck in RUNNING for >30 min are automatically reset to FAILED (staleness guard).
    - Scheduling is enabled by default but disabled in tests via `app.scheduling.enabled: false`.
    - `@EnableScheduling` is applied on `EventsImporterApplication`.
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
- Centralized versions in root `build.gradle.kts` (`extra["java.version"]`, `extra["jsoup.version"]`, `extra["kotest.version"]`,
  `extra["kotlin-logging.version"]`, `extra["mockk.version"]`, `extra["slugify.version"]`, `extra["spring-modulith.version"]`,
  `extra["springdoc.version"]`); plugin versions in `settings.gradle.kts` `pluginManagement`.
- Use `val` for injected dependencies; constructor injection only (no field injection).
- Application config files use **`.yaml`** extension (not `.yml`).
- Kotlin compiler flags: `-Xjsr305=strict` (all modules) and `-Xannotation-default-target=param-property` (BFF + importer) are set in `compilerOptions`.
- **Kover** (`org.jetbrains.kotlinx.kover`) is configured for code coverage reports. Run `./gradlew koverLog` for a console summary
  or `./gradlew koverHtmlReport` for detailed HTML reports.
- **Kotlin idioms** (per [official coding conventions](https://kotlinlang.org/docs/coding-conventions.html)):
    - **Trailing commas** at declaration sites (constructor params, function params, enum entries, collection literals) — produces cleaner VCS diffs.
    - **Expression bodies** — prefer `fun foo() = expr` over `fun foo() { return expr }` for single-expression functions.
    - **Named arguments** — use when a function has multiple parameters of the same type or Boolean parameters whose meaning isn't obvious from context.
    - **Immutable collection interfaces** — declare parameters and return types as `List`, `Set`, `Map` (not `MutableList` etc.) when the collection is not
      mutated. Use `listOf()`, `setOf()`, `mapOf()` factory functions.
    - **Expression form of control flow** — prefer `if`/`when`/`try` as expressions returning a value over imperative `return` inside branches.
    - **Higher-order functions over loops** — prefer `filter`, `map`, `flatMap`, `associate` over imperative `for` loops where readability is equal or better.
    - **Default parameter values** — prefer over function overloads.
    - **Scope functions** — use `let`, `apply`, `also`, `run`, `with` appropriately; avoid deep nesting of scope functions.

## Testing Patterns

- **JUnit 5** + **WebTestClient** for reactive endpoint tests (see `BaseControllerTest.kt`).
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
- **MockK**: The importer uses `io.mockk:mockk` for mocking in Kotlin tests (preferred over Mockito). Used for unit-testing services with injected dependencies.
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

| Purpose                               | Path                                                                                                      |
|---------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Root build config & shared versions   | `build.gradle.kts`                                                                                        |
| Plugin versions & module includes     | `settings.gradle.kts`                                                                                     |
| Gradle daemon JVM args                | `gradle.properties`                                                                                       |
| Dev database (Postgres)               | `compose.yaml`                                                                                            |
| Detekt rule overrides                 | `detekt.yml`                                                                                              |
| OWASP CVE false-positive suppressions | `owasp-suppressions.xml`                                                                                  |
| CI: backend build & test              | `.github/workflows/build-backend.yml`                                                                     |
| CI: frontend build & test             | `.github/workflows/build-frontend.yml`                                                                    |
| CI: dependency review (PR)            | `.github/workflows/dependency-review.yml`                                                                 |
| CI: dependency graph submission       | `.github/workflows/dependency-submission.yml`                                                             |
| Dependabot config                     | `.github/dependabot.yml`                                                                                  |
| Commit message prompt                 | `.github/prompts/commit-message.prompt.md`                                                                |
| Squash commit message prompt          | `.github/prompts/squash-commit-message.prompt.md`                                                         |
| Code review prompt                    | `.github/prompts/code-review.prompt.md`                                                                   |
| Shared domain module marker           | `events-core/src/.../EventsCoreModule.kt`                                                                 |
| Domain data classes                   | `events-core/src/.../artist/`, `event/`, `genretag/`, `promoter/`, `venue/`                               |
| Price normalization utility           | `events-core/src/.../event/MoneyExtensions.kt`                                                            |
| Initial DB migration                  | `events-importer/src/main/resources/db/migration/V001__create_initial_schema.sql`                         |
| Global exception handler              | `events-importer/src/.../GlobalExceptionHandler.kt`                                                       |
| Slug generator utility                | `events-importer/src/.../slug/SlugGenerator.kt`                                                           |
| Genre normalizer utility              | `events-importer/src/.../genretag/GenreNormalizer.kt`                                                     |
| Shared scraping utilities             | `events-importer/src/.../scraper/ScrapingExtensions.kt`                                                   |
| Shared date/time parsing              | `events-importer/src/.../scraper/DateParsingExtensions.kt`                                                |
| Shared event mapping utilities        | `events-importer/src/.../scraper/EventMappingExtensions.kt`                                               |
| WebFlux Pageable resolver config      | `events-importer/src/.../WebFluxConfiguration.kt`                                                         |
| Base integration test class           | `events-importer/src/test/.../BaseControllerTest.kt`                                                      |
| Full lifecycle integration test       | `events-importer/src/test/.../event/FullLifecycleIntegrationTest.kt`                                      |
| Testcontainers setup (BFF)            | `events-bff/src/test/.../PostgresTestcontainersConfiguration.kt`                                          |
| Testcontainers setup (importer)       | `events-importer/src/test/.../PostgresTestcontainersConfiguration.kt`                                     |
| Modularity verification (BFF)         | `events-bff/src/test/.../ModularityTests.kt`                                                              |
| Modularity verification (importer)    | `events-importer/src/test/.../ModularityTests.kt`                                                         |
| Modularity verification (core)        | `events-core/src/test/.../ModularityTests.kt`                                                             |
| ADR: Reactive stack                   | `docs/adr/ADR-001_REACTIVE_STACK.md`                                                                      |
| ADR: R2DBC query derivation limits    | `docs/adr/ADR-002_R2DBC_QUERY_DERIVATION.md`                                                              |
| ADR: Entity/domain separation         | `docs/adr/ADR-003_ENTITY_DOMAIN_SEPARATION.md`                                                            |
| ADR: Dedicated database schema        | `docs/adr/ADR-004_DEDICATED_DATABASE_SCHEMA.md`                                                           |
| ADR: Migrations owned by importer     | `docs/adr/ADR-005_MIGRATIONS_OWNED_BY_IMPORTER.md`                                                        |
| ADR: Spring Modulith                  | `docs/adr/ADR-006_SPRING_MODULITH.md`                                                                     |
| ADR: Web scraping strategy            | `docs/adr/ADR-007_WEB_SCRAPING_STRATEGY.md`                                                               |
| ADR: Import job scheduling            | `docs/adr/ADR-008_IMPORT_JOB_SCHEDULING.md`                                                               |
| ADR: Optimistic locking (event src)   | `docs/adr/ADR-009_OPTIMISTIC_LOCKING_EVENT_SOURCE.md`                                                     |
| ADR: Frontend styling framework       | `docs/adr/ADR-010_FRONTEND_STYLING_FRAMEWORK.md`                                                          |
| ADR: Event-calendar library           | `docs/adr/ADR-011_CALENDAR_LIBRARY.md`                                                                    |
| Frontend entry point                  | `events-frontend/src/main.ts`                                                                             |
| IntelliJ HTTP Client requests         | `http/importer/` (admin) and `http/bff/` (public read) `.http` files + shared `http/http-client.env.json` |
