# Event Checker

[![Build & Test Backend](https://github.com/enorm-labs/event-checker/actions/workflows/build-backend.yml/badge.svg)](https://github.com/enorm-labs/event-checker/actions/workflows/build-backend.yml)
[![Build & Test Frontend](https://github.com/enorm-labs/event-checker/actions/workflows/build-frontend.yml/badge.svg)](https://github.com/enorm-labs/event-checker/actions/workflows/build-frontend.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)
[![Status](https://img.shields.io/badge/Status-In%20Development-orange.svg)](#project-status)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Vue.js](https://img.shields.io/badge/Vue.js-3-4FC08D.svg?logo=vuedotjs&logoColor=white)](https://vuejs.org)

A simple app/website for checking and finding music events in Berlin (scope might be extended in the future).

## Project Status

🚧 **In Development** — This project is not yet live or deployed to any production environment.
The database schema is still evolving and may change without migration compatibility between versions.
All schema changes are consolidated into a single initial migration (`V001`) until the first production release.

## TODO

See [TODO.md](./TODO.md)

## Vision / Roadmap / Ideas

See [VISION ROADMAP_IDEAS.md](./docs/VISION_ROADMAP_IDEAS.md)

## Architecture

* Frontend: Vue app (see [events-frontend](./events-frontend))
* Backend for Frontend: Spring Boot app with Kotlin, WebFlux and R2DBC (read-only?) (see [events-bff](./events-bff))
* Importer: Spring Boot app with Kotlin, WebFlux and R2DBC (see [events-importer](./events-importer))
* Database: PostgreSQL
* Search: Elasticsearch (maybe later)
* MGMT API and Frontend (maybe later)
* Android app (maybe later)
* AI agent / MCP server (maybe later)

## Development

For frontend development, see [events-frontend/README.md](./events-frontend/README.md).

### Setup JDK

Install SDKMAN to manage Java versions: https://sdkman.io/

```
# Use the Java version specified in .sdkmanrc
sdk env
```

### Git Hooks (gitleaks)

[Gitleaks](https://github.com/gitleaks/gitleaks) runs as a pre-commit hook via
[pre-commit](https://pre-commit.com/) to prevent secrets from being committed.

```bash
# Install the pre-commit framework (macOS)
brew install pre-commit

# Install the git hook
pre-commit install
```

The hook runs automatically on every `git commit`. To scan manually without committing:

```bash
# Scan all files tracked by git
pre-commit run gitleaks --all-files

# Scan the entire git history for leaked secrets (requires gitleaks: brew install gitleaks)
gitleaks detect --source . --verbose
```

### Build

```
./gradlew clean build
```

### Run

Start applications via IntelliJ or via Gradle like this:

```
./gradlew bootRun
```

This will also start the services (database) defined in the [compose.yaml](./compose.yaml) file via
Spring's [Docker Compose Support](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.docker-compose).

The PostgreSQL database is exposed on host port **56298** by default (mapped from container port 5432).
Spring Boot discovers the port automatically. To connect manually (e.g. via `psql` or a database GUI),
use `localhost:56298` with the credentials defined in `compose.yaml` (`admin`/`admin`, database `event_checker`).

If port 56298 is already in use, override it via the `POSTGRES_HOST_PORT` environment variable:

```bash
POSTGRES_HOST_PORT=5555 ./gradlew :events-importer:bootRun
```

To reset the local database (e.g. to start fresh), stop the app and remove the Docker volume:

```bash
docker compose down --volumes
```

The next `bootRun` will recreate the database and re-run all Flyway migrations.

### IntelliJ HTTP Client

The [`http/`](./http) directory contains IntelliJ HTTP Client request files covering all admin endpoints
(venues, artists, promoters, events) as well as health checks and OpenAPI specs.

#### From IntelliJ IDEA

1. Start the importer: `./gradlew :events-importer:bootRun`
2. Open any `.http` file in IntelliJ → select the **local** environment from the dropdown
3. Click the green ▶ play button next to a request to execute it
4. Create requests store response IDs automatically (e.g. `{{venue_id}}`), so subsequent
   update, delete, and event requests can reference them without manual copy-paste

#### From the command line (ijhttp CLI)

The same `.http` files can be executed outside IntelliJ using the
[HTTP Client CLI](https://www.jetbrains.com/help/idea/http-client-cli.html) (`ijhttp`).
No IntelliJ Ultimate license is required.

```bash
# Install (macOS)
brew install ijhttp

# Run the full CRUD lifecycle scenario
./gradlew httpTest
```

The `httpTest` Gradle task runs `full-lifecycle.http` against the **local** environment
with verbose logging. The importer must be running on port 8081 before you execute it.

You can also invoke `ijhttp` directly for individual files:

```bash
cd http
ijhttp --env-file http-client.env.json --env local venues.http
ijhttp --env-file http-client.env.json --env local -L VERBOSE full-lifecycle.http
```

Docs: https://www.jetbrains.com/help/idea/http-client-cli.html

### Swagger UI (OpenAPI)

When running an application locally, Swagger UI is available at:

* **events-bff**: http://localhost:8080/webjars/swagger-ui/index.html
* **events-importer**: http://localhost:8081/webjars/swagger-ui/index.html

The OpenAPI spec (JSON) is served at `/v3/api-docs` on the respective port.

### Check for dependency updates

```
./gradlew dependencyUpdates
```

Docs: https://github.com/ben-manes/gradle-versions-plugin

### Update the Gradlew Wrapper

```
# The following command upgrades the Wrapper to the latest version:
./gradlew wrapper --gradle-version latest

# The following command upgrades the Wrapper to a specific version:
./gradlew wrapper --gradle-version 9.5.0
```

Docs: https://docs.gradle.org/current/userguide/gradle_wrapper.html

### Lint and Formatting (ktlint)

```
## Lint
./gradlew ktlintCheck

## Format
./gradlew ktlintFormat
```

### Static Analysis (detekt)

```bash
# Run detekt on all modules
./gradlew detekt

# Generate a default detekt.yml config file (optional, for rule customization)
./gradlew detektGenerateConfig
```

HTML reports are generated at `build/reports/detekt/`.

Docs: https://detekt.dev/

### Test Coverage (Kover)

```bash
# Print line coverage per module to the console
./gradlew koverLog

# Generate detailed HTML reports (build/reports/kover/html/index.html)
./gradlew koverHtmlReport

# Generate XML reports (for CI tools like Codecov or SonarQube)
./gradlew koverXmlReport
```

Docs: https://kotlin.github.io/kotlinx-kover/

### Dependency CVE Scanning (OWASP Dependency-Check)

Scans all project dependencies against the [National Vulnerability Database (NVD)](https://nvd.nist.gov/)
for known CVEs. The build fails if a vulnerability with CVSS score ≥ 7 (HIGH) is found.

```bash
# Run the aggregated scan across all modules
./gradlew dependencyCheckAggregate
```

Reports are generated at `build/reports/`:

- `dependency-check-report.html` — detailed HTML report
- `dependency-check-report.sarif` — SARIF for GitHub Code Scanning

False positives can be suppressed in [`owasp-suppressions.xml`](./owasp-suppressions.xml).

Docs: https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/

#### NVD API Key (recommended)

The NVD rate-limits unauthenticated requests, making the initial database download very slow (~10+ min).
A free API key brings this down to ~1 minute.

1. Request a key at https://nvd.nist.gov/developers/request-an-api-key
2. For **local development**, set it as an environment variable:
   ```bash
   export NVD_API_KEY=your-key-here
   ```
3. For **CI (GitHub Actions)**, add it as a repository secret named `NVD_API_KEY`
   (Settings → Secrets and variables → Actions → New repository secret).

