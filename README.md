# Event Checker

[![Build & Test Backend](https://github.com/enorm-labs/event-checker/actions/workflows/build-backend.yml/badge.svg)](https://github.com/enorm-labs/event-checker/actions/workflows/build-backend.yml)
[![Build & Test Frontend](https://github.com/enorm-labs/event-checker/actions/workflows/build-frontend.yml/badge.svg)](https://github.com/enorm-labs/event-checker/actions/workflows/build-frontend.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)
[![Status](https://img.shields.io/badge/Status-In%20Development-orange.svg)](#project-status)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Vue.js](https://img.shields.io/badge/Vue.js-3-4FC08D.svg?logo=vuedotjs&logoColor=white)](https://vuejs.org)

A simple app/website for checking and finding music events in Berlin (scope might be extended in the future).

## Overview

What Event Junkie is, what it does, the problems it solves, and the current feature set:
see [PRODUCT_OVERVIEW.md](./docs/PRODUCT_OVERVIEW.md).

## Built with AI

This project is developed heavily with AI assistance — most of the code in this repository was written by AI coding agents (primarily
[Claude Code](https://claude.com/claude-code)), guided by the prompts and skills under [`.github/prompts/`](./.github/prompts) and
[`AGENTS.md`](./AGENTS.md).

The vision, the product ideas, the architecture decisions and the priorities are mine. The agents implement against them; every change goes through review
before it lands.

## Project Status

🚧 **In Development** — This project is not yet live or deployed to any production environment. The database schema is still evolving and may change without
migration compatibility between versions. All schema changes are consolidated into a single initial migration (`V001`) until the first production release.

## TODO

See [TODO.md](./TODO.md)

## Vision / Roadmap / Ideas

See [VISION_ROADMAP_IDEAS.md](./docs/VISION_ROADMAP_IDEAS.md)

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

The PostgreSQL database is exposed on host port **56298** by default (mapped from container port 5432). Spring Boot discovers the port automatically. To connect
manually (e.g. via `psql` or a database GUI), use `localhost:56298` with the credentials defined in `compose.yaml` (`admin`/`admin`, database `event_checker`).

If port 56298 is already in use, override it via the `POSTGRES_HOST_PORT` environment variable:

```bash
POSTGRES_HOST_PORT=5555 ./gradlew :events-importer:bootRun
```

Database contents live on the named volume `postgres-data`, so they survive the container being stopped and recreated. To reset the local database (e.g. to
start fresh), stop the app and remove that volume:

```bash
docker compose down --volumes
```

The next `bootRun` will recreate the database and re-run all Flyway migrations.

### The `local` profile (logging to a file)

Both services define a `local` Spring profile whose only effect is to mirror the console output to a file, so an import or request run can be grepped afterwards
instead of scrolled in the IDE console:

| Service           | Log file                                     |
|-------------------|----------------------------------------------|
| `events-importer` | `events-importer/build/dev-env/importer.log` |
| `events-bff`      | `events-bff/build/dev-env/bff.log`           |

```bash
./gradlew :events-importer:bootRun --args='--spring.profiles.active=local'
```

In IntelliJ, set **Active profiles: `local`** in the run configuration. The paths are relative to each module directory (`bootRun`'s working directory) and land
under `build/`, which is gitignored.

The profile gate is deliberate: on a container platform the log belongs on stdout, where the platform collects it, and a file appender would instead write into
the container's in-memory filesystem. Note that `scripts/dev-env.sh` does not need the profile — it redirects each service's stdout to
`build/dev-env/<service>.log` at the repository root itself.

### Running the stack with `dev-env.sh`

[`scripts/dev-env.sh`](./scripts/dev-env.sh) starts and stops the local stack without remembering docker/gradle/npm incantations. Run it with no arguments for
the full command list.

```bash
scripts/dev-env.sh up all       # importer + bff + frontend, each waited on until it answers
scripts/dev-env.sh status       # database / importer / bff / frontend
scripts/dev-env.sh down all     # add --db to stop Postgres too
```

`up` and `down` take one or more of `importer` (the default) · `bff` · `frontend` · `all`. Each service logs to `build/dev-env/<service>.log`. The frontend
proxies `/api` to the BFF, so starting it alone renders the app but every request 502s.

The script also covers the importer-specific workflow — `seed-all`, `seed-one`, `import <slug>`, `snapshot`, `diff-snapshot`, `check <slug>` and `psql <sql>`.

### Parallel work with Git worktrees

A [git worktree](https://git-scm.com/docs/git-worktree) is a second working directory on its own branch that shares the repository's `.git` directory and
remote. Two worktrees means two checkouts whose files can't collide, which is what makes it practical to run two coding sessions — or two AI agents — on two
importers at the same time. Background:
[Claude Code: run parallel sessions with worktrees](https://code.claude.com/docs/en/worktrees) ·
[Git worktrees for parallel AI coding](https://www.mindstudio.ai/blog/git-worktrees-parallel-ai-coding-agents).

**What is and isn't isolated here**: source files, branch and Gradle `build/` output are per worktree. The local runtime is **not** — Postgres (host port
`56298`), the importer (`8081`), the BFF (`8080`) and the frontend (`5173`) are fixed, shared, single-tenant resources. So the rule is: *edit in parallel, run
the stack one worktree at a time.*

#### 1. Create a worktree

Claude Code can create and enter one for you. This puts a fresh checkout in `.claude/worktrees/tresor` on a new branch `worktree-tresor`, branched from
`origin/main`, and starts the session there:

```bash
claude --worktree tresor
```

Run it again with another name in a second terminal for a second isolated session. (You can also just ask Claude to "work in a worktree" mid-session.)

For work that ends in a pull request, plain git is usually nicer, because you pick the branch name that `/open-pr` will push — it reuses the branch it finds
rather than cutting a new one, so a `--worktree` session would open its PR from `worktree-tresor`:

```bash
git worktree add ../event-checker-tresor -b feat/tresor origin/main
cd ../event-checker-tresor
claude
```

`.claude/worktrees/` is gitignored, so Claude-created worktrees never show up as untracked files in the main checkout.

IntelliJ can do the same from the UI ([JetBrains docs](https://www.jetbrains.com/help/idea/use-git-worktrees.html)):

* **Create** — Git tool window (<kbd>⌘9</kbd>) → **Worktrees** → **New Worktree**, or main menu **Git | New Worktree**. Pick the source branch (`origin/main`),
  a project name and a location *outside* this repository, e.g. `../event-checker-tresor`. The worktree opens as its own project window. The same branch cannot
  be checked out in two worktrees, so give each one a new branch.
* **Switch** — double-click a worktree in the same **Worktrees** tab; or right-click a branch in the **Log** tab and choose **Open Worktree**.
* **Remove** — select it in **Worktrees** and click **Delete** (not possible for the main or the currently open worktree, and commit first). If you deleted the
  directory by hand, the entry shows as *Prunable* — **Prune** clears all of them.

The usual caveat about `.idea/workspace.xml` making IntelliJ treat every worktree as one project does not apply here: all of `.idea` is gitignored. Each
worktree window therefore needs its own SDK and run configurations — see the next two steps.

#### 2. Set the worktree up

A worktree checks out tracked files only, so each one needs its own environment:

```bash
sdk env                                  # .sdkmanrc is tracked — this just works
cd events-frontend && npm ci             # only if you need the frontend; dev-env.sh refuses to start it without node_modules
```

* Gradle's `build/` directories are per worktree, so the first build there compiles from scratch.
* The gitleaks pre-commit hook lives in the shared `.git` directory and is therefore already active in every worktree — no second `pre-commit install`.
* This repo has no gitignored-but-required files (no `.env`), so no [`.worktreeinclude`](https://code.claude.com/docs/en/worktrees) is needed. Add one only if
  that changes.

#### 3. Point the worktree at the existing database

**This is the one step that bites.** Docker Compose derives its project name from the directory holding `compose.yaml`, and both `bootRun` and
`scripts/dev-env.sh` pass the *worktree's* copy. So a worktree at `.claude/worktrees/tresor` would come up as compose project `tresor` — a second Postgres
container on a brand-new empty `tresor_postgres-data` volume, clashing with the main checkout on host port `56298`. An empty database also makes
`diff-snapshot` report every existing source as `GONE`.

Export the main checkout's project name in every worktree shell, and compose reuses the running container, its volume and its seeded data instead:

```bash
export COMPOSE_PROJECT_NAME=event-checker

# with it     →  Container event-checker-postgres-1  Running        (reused, data intact)
# without it  →  Volume tresor_postgres-data  Creating              (empty DB, and port 56298 is already allocated)
```

Put it in the worktree's shell profile, direnv `.envrc`, or the IntelliJ run configuration — anywhere it is guaranteed to be set before the first `bootRun`.

#### 4. Take turns on the stack

Everything that only touches files runs in parallel across as many worktrees as you like: writing scrapers, fixtures and unit tests, `ktlintFormat`,
`detekt`, `:events-importer:test`. Everything with a port or a row in the database is serialized:

* Only one worktree may hold the stack. `scripts/dev-env.sh down` in the first, then `up` in the second — `dev-env.sh` overrides such as `IMPORTER_HOST` only
  change the URL it polls, not the port the JVM binds, so a second importer cannot simply move to `8091`.
* `bootRun` does not hot-reload: whichever worktree started the JVM is the code being smoke-tested. Restart after switching.
* Never let two worktrees import at once. `snapshot` / `diff-snapshot` count events per source across the whole database, so the other session's import lands in
  your regression diff as an unexplained delta.
* Two concurrent Gradle builds mean two daemons at `-Xmx2g` each — two or three active worktrees is a sane ceiling on a laptop.

#### 5. Expect conflicts in the shared files

One worktree = one venue = one PR (that is exactly the `/next-importer` contract). Every importer PR touches the same handful of shared files, so resolve these
deliberately rather than accepting either side:

| File                                         | What conflicts                                                                                               |
|----------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `docs/EVENT_DATA_SOURCES.md`                 | the status **count** table plus the moved row — recount after rebasing; both sides bump the same numbers     |
| `http/importer/dev-seed.http`                | the alphabetical header list and the venue block — "keep both" can silently fuse two blocks, rebuild by hand |
| `events-importer/.../scraper/EventSource.kt` | one new enum entry each                                                                                      |
| `TODO.md`                                    | the bugs list, when a smoke test found something                                                             |

Rebase feature branches onto `main`; don't merge `main` into them — PRs here are merged with "Rebase and merge", which a merge commit blocks.

#### 6. Clean up

```bash
git worktree list
git worktree remove ../event-checker-tresor   # add --force if it still holds uncommitted work
git worktree prune                            # drop metadata for directories deleted by hand
```

`git worktree remove` deletes the directory but keeps the branch. Claude's own exit prompt for a `--worktree` session offers to remove the branch too, so
decline it unless the work is pushed or merged. Sessions started with `-p` are never cleaned up automatically.

### IntelliJ HTTP Client

The [`http/`](./http) directory contains IntelliJ HTTP Client request files, split by service:

- [`http/importer/`](./http/importer) — the importer's admin CRUD endpoints (venues, artists, promoters, events, event sources, dev seed) plus its
  health/OpenAPI checks.
- [`http/bff/`](./http/bff) — the BFF's public read API (events, venues, artists, genres) plus its health/OpenAPI checks.

The shared `http-client.env.json` lives at the `http/` root (IntelliJ resolves it from parent directories), defining `importer-host` and `bff-host`.

#### From IntelliJ IDEA

1. Start the relevant service: `./gradlew :events-importer:bootRun` and/or `./gradlew :events-bff:bootRun`
2. Open any `.http` file in IntelliJ → select the **local** environment from the dropdown
3. Click the green ▶ play button next to a request to execute it
4. Create requests store response IDs automatically (e.g. `{{venue_id}}`), so subsequent update, delete, and event requests can reference them without manual
   copy-paste

#### From the command line (ijhttp CLI)

The same `.http` files can be executed outside IntelliJ using the
[HTTP Client CLI](https://www.jetbrains.com/help/idea/http-client-cli.html) (`ijhttp`). No IntelliJ Ultimate license is required.

```bash
# Install (macOS)
brew install ijhttp

# Run the full CRUD lifecycle scenario
./gradlew httpTest
```

The `httpTest` Gradle task runs `full-lifecycle.http` against the **local** environment with verbose logging. The importer must be running on port 8081 before
you execute it.

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
./gradlew wrapper --gradle-version 9.6.1
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

The NVD rate-limits unauthenticated requests, making the initial database download very slow (~10+ min). A free API key brings this down to ~1 minute.

1. Request a key at https://nvd.nist.gov/developers/request-an-api-key
2. For **local development**, set it as an environment variable:
   ```bash
   export NVD_API_KEY=your-key-here
   ```
3. For **CI (GitHub Actions)**, add it as a repository secret named `NVD_API_KEY`
   (Settings → Secrets and variables → Actions → New repository secret).


### Dependency Licences & Open-Source Notices

Every runtime dependency's licence is checked against a policy, and the full list is published on the site at `/legal/notices`.

**Two checks, one policy.** They exist separately because the two ecosystems report licence names in different vocabularies — npm uses SPDX identifiers
(`BSD-2-Clause`), the Gradle plugin uses its normaliser's prose names (`The 2-Clause BSD License`). Change them together.

```bash
# JVM runtime dependencies (all three Gradle modules)
./gradlew checkLicense --no-configuration-cache

# Frontend production npm dependencies
cd events-frontend && npm run check:licenses
```

- Policy files: [`config/allowed-licenses-jvm.json`](./config/allowed-licenses-jvm.json) and [`config/allowed-licenses-npm.json`](./config/allowed-licenses-npm.json).
- A third gate, [`dependency-review.yml`](./.github/workflows/dependency-review.yml), carries a deny-list applied to *newly introduced* dependencies at PR time.
- **Do not widen an allow-list to make a build pass.** AGPL, GPL without the Classpath Exception, and source-available licences (SSPL, BUSL, Elastic-2.0) are
  not acceptable for a public network service whose own source is Apache-2.0. AGPL is the one to watch: its § 13 obligation fires on *network interaction*, not
  distribution. See [docs/FOOTER_AND_LEGAL_PLAN.md §9.2](./docs/FOOTER_AND_LEGAL_PLAN.md).

**Regenerating the notices page.** `events-frontend/src/assets/notices.json` is generated and committed — never hand-edited. Regenerate it whenever
dependencies change on either side:

```bash
./gradlew generateLicenseReport --no-configuration-cache   # writes build/reports/dependency-license/licenses.json
cd events-frontend && npm run generate:notices             # merges both ecosystems into src/assets/notices.json
```

The `--no-configuration-cache` flag is required: the licence-report plugin is not configuration-cache compatible (same as `dependencyCheckAggregate` — see the
note in [`gradle.properties`](./gradle.properties)). The generator writes no timestamp, so re-running it with unchanged dependencies produces an identical file
and an empty diff.

It is committed rather than generated at build time because the frontend is not a Gradle subproject: its build must not have to invoke Gradle, and the page then
works under `npm run dev` with nothing else run first.
