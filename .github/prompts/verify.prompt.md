# Verify

Run the full pre-PR verification sequence for both backend and frontend, then report what passed, what failed, and the
first actionable failure.

## What it runs

### Backend (from repo root)

```bash
./gradlew ktlintCheck detekt build koverLog
```

Gradle's `build` task already depends on `check` (which runs tests), so this single invocation covers compile, ktlint,
detekt, tests, and Kover thresholds. `koverLog` then prints the coverage summary to stdout so you can see it without
opening the HTML report.

### Frontend (from `events-frontend/`)

```bash
cd events-frontend
npm run type-check
npm run lint
npm run test:unit -- --run
```

- `type-check` — `vue-tsc --build`
- `lint` — runs both `oxlint --fix` and `eslint --fix --cache` (via `run-s lint:*`)
- `test:unit -- --run` — vitest in single-run mode (default is watch)

Skip E2E (`test:e2e`) here; it's slow and requires a running stack. Run it manually if needed.

## How to run the skill

1. Run the **backend** sequence first. If it fails, surface the first failing task, quote the actual error lines from
   Gradle's output, and stop — don't run the frontend until backend is green (the user usually wants to fix one stack
   at a time).
2. If backend passes, run the **frontend** sequence next.
3. If `ktlintCheck` fails, suggest `./gradlew ktlintFormat` to auto-fix before re-running — per AGENTS.md, ktlint
   auto-format should be tried first.
4. If `lint` fails on the frontend, note that both oxlint and eslint already pass `--fix`, so remaining failures are
   genuine issues that need manual edits.
5. Report at the end with a compact summary:

   ```
   Backend:  ktlintCheck ✓  detekt ✓  build ✓  koverLog ✓
   Frontend: type-check ✓  lint ✓  test:unit ✓
   ```

   On failure, replace the ✓ with ✗ for the failing step, list the others as skipped if you stopped early, and quote
   the first useful error line below the summary.

## Gotchas

- **Java 25 required** — if `./gradlew` fails with an unsupported class file version, run `sdk env` to pick up the
  pinned JDK from `.sdkmanrc`.
- **Database isn't required** for this skill — the build uses Testcontainers for tests; the dev `compose.yaml` Postgres
  is only needed for `bootRun`.
- **NVD_API_KEY** is *not* needed here — `dependencyCheckAggregate` is not part of `build`.
- **Backend-only or frontend-only changes**: if the diff touches only `events-frontend/`, skip the backend sequence;
  if it touches only backend modules, skip the frontend sequence. Use `git --no-pager diff --name-only main..HEAD`
  (or against the merge-base) to decide.