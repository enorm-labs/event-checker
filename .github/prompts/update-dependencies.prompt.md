# Update Dependencies

Update project dependencies to their latest stable versions, applying only to versions managed by this project — never
override versions controlled by Spring Boot or Spring Dependency Management BOMs.

## Important

Always run git commands with the pager disabled (`git --no-pager ...`) to prevent hanging on interactive output.

## Step 1: Generate the Dependency Update Report

Run the Gradle Versions Plugin to detect available updates:

```bash
./gradlew dependencyUpdates
```

This produces a report at `build/dependencyUpdates/report.txt` listing all dependencies with available updates.
Read and analyze the full report.

## Step 2: Identify Which Versions We Manage

This project has two categories of dependency versions:

### ✅ Managed by us (update these)

Versions explicitly pinned in **`settings.gradle.kts`** (plugin versions) and **root `build.gradle.kts`** (`extra[...]`
properties). Currently:

**Root `build.gradle.kts` — library versions:**

| Property                  | Dependency                             |
|---------------------------|----------------------------------------|
| `jsoup.version`           | `org.jsoup:jsoup`                      |
| `kotest.version`          | `io.kotest:kotest-assertions-core`     |
| `kotlin-logging.version`  | `io.github.oshai:kotlin-logging-jvm`   |
| `mockk.version`           | `io.mockk:mockk`                       |
| `slugify.version`         | `com.github.slugify:slugify`           |
| `spring-modulith.version` | `org.springframework.modulith:*` (BOM) |
| `springdoc.version`       | `org.springdoc:springdoc-openapi-*`    |

**`settings.gradle.kts` — plugin versions:**

| Plugin ID                                   | Dependency                          |
|---------------------------------------------|-------------------------------------|
| `kotlin("jvm")` / `kotlin("plugin.spring")` | Kotlin compiler & plugins           |
| `org.springframework.boot`                  | Spring Boot Gradle plugin           |
| `io.spring.dependency-management`           | Spring Dependency Management plugin |
| `org.jetbrains.kotlinx.kover`               | Kover code coverage plugin          |
| `org.jlleitschuh.gradle.ktlint`             | ktlint Gradle plugin                |
| `dev.detekt`                                | Detekt static analysis plugin       |
| `com.github.ben-manes.versions`             | Gradle Versions Plugin              |
| `org.owasp.dependencycheck`                 | OWASP Dependency-Check plugin       |

Also check whether the **ktlint version** (`version = "..."` inside the `configure<KtlintExtension>` block in root
`build.gradle.kts`) has a newer stable release.

### ❌ Managed by BOMs (do NOT update these)

Dependencies whose versions come from the **Spring Boot BOM** (`org.springframework.boot` plugin) or the
**Spring Modulith BOM**. These include, but are not limited to:

- `org.springframework.boot:spring-boot-starter-*`
- `org.springframework:spring-*`
- `org.jetbrains.kotlin:kotlin-*` (version aligned by Kotlin plugin)
- `org.jetbrains.kotlinx:kotlinx-coroutines-*`
- `io.projectreactor.kotlin:reactor-kotlin-extensions`
- `tools.jackson.module:jackson-module-kotlin`
- `org.flywaydb:flyway-*`
- `org.postgresql:postgresql` / `org.postgresql:r2dbc-postgresql`
- `org.testcontainers:*`
- `org.junit.*` / `junit-platform-*`

**Rule of thumb**: If a dependency is declared _without_ an explicit version string (no `${property("...")}` or
hardcoded version), its version comes from a BOM and must NOT be overridden.

## Step 3: Filter for Stable Versions Only

The `dependencyUpdates` report may include milestone, alpha, beta, and RC releases. **Only update to stable releases.**

Reject any version containing these indicators (case-insensitive):
`alpha`, `beta`, `rc`, `cr`, `m1`, `m2`, `m3`, `dev`, `snapshot`, `eap`, `-M`, `preview`.

## Step 4: Cross-check Compatibility

Before applying updates, verify compatibility:

- **Spring Boot ↔ Spring Modulith**: Check the
  [Spring Modulith compatibility matrix](https://github.com/spring-projects/spring-modulith#compatibility-matrix) to
  ensure the Spring Modulith version is compatible with the Spring Boot version.
- **Kotlin ↔ Spring Boot**: Verify the Kotlin version is supported by the Spring Boot version (check the Spring Boot
  release notes).
- **Major version bumps**: For any major version upgrade (e.g., 5.x → 6.x), check the migration guide and note any
  breaking changes. Flag these for the user instead of silently applying them.

## Step 5: Apply Updates

Edit the version strings in the appropriate files:

- **Library versions** → root `build.gradle.kts` (`extra["..."]` properties)
- **Plugin versions** → `settings.gradle.kts` (`pluginManagement { plugins { ... } }`)
- **ktlint version** → root `build.gradle.kts` (`configure<KtlintExtension> { version = "..." }`)

## Step 6: Verify the Build

After applying updates, run:

```bash
./gradlew clean build
```

If the build fails:

1. Read the error output carefully.
2. Check if the failure is caused by the update (breaking API change, removed method, etc.).
3. Fix straightforward issues (import changes, minor API adaptations).
4. For complex breaking changes, revert that specific update and flag it for the user.

## Frontend Dependencies (`events-frontend/`)

The frontend is a standalone npm project — not part of the Gradle build. Update it separately.

### Step 7: Check for Outdated Frontend Dependencies

```bash
cd events-frontend
npm outdated
```

This shows a table of all dependencies with their current, wanted, and latest versions.

### Step 8: Update Versions in `package.json`

Manually update the version strings in `events-frontend/package.json` to the latest stable versions reported by
`npm outdated`. This project uses exact (pinned) versions — no `^` or `~` prefixes.

**Rules:**

- Only update to **stable releases** — skip versions containing `alpha`, `beta`, `rc`, `next`, `canary`, `dev`,
  `snapshot`, `preview`.
- **Major version bumps** (e.g., 3.x → 4.x): Check the migration guide and flag breaking changes for the user instead
  of silently applying them.
- Keep `oxlint` and `eslint-plugin-oxlint` versions in sync (they share the same release cadence).
- Keep `vue` and `vue-router` compatible with each other (check Vue ecosystem compatibility).

### Step 9: Install Updated Dependencies

After editing `package.json`, run:

```bash
npm update --save --save-exact
```

This installs the updated versions and updates `package-lock.json`.

### Step 10: Verify the Frontend Build

```bash
npm run build
```

If the build fails:

1. Read the error output carefully.
2. Check if the failure is caused by the update (breaking API change, removed type, etc.).
3. Fix straightforward issues (import changes, minor API adaptations).
4. For complex breaking changes, revert that specific update and flag it for the user.

Optionally run linting and unit tests:

```bash
npm run lint
npm run test:unit
```

## Output Summary

After completing the update, provide a summary table:

| Dependency | Previous Version | New Version | Location                                   |
|------------|------------------|-------------|--------------------------------------------|
| ...        | ...              | ...         | `build.gradle.kts` / `settings.gradle.kts` |
| ...        | ...              | ...         | `events-frontend/package.json`             |

Also note:

- Any dependencies that were **skipped** because only pre-release versions were available.
- Any **major version bumps** that were applied, with a brief note on breaking changes (if any).
- Any dependencies already at their **latest stable version** (no update needed).
