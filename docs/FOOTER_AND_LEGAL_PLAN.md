# Plan — Site Footer, Version Display & Legal Pages

> Status: **Phases 1–6 implemented** (2026-08-07); Phase 7 (localisation) still proposal.
> Related: [TODO.md §Legal / Compliance](../TODO.md) · [BRANDING.md](BRANDING.md) · [ADR-012 (cloud platform)](adr/ADR-012_CLOUD_PLATFORM.md) ·
> [PRODUCT_OVERVIEW.md](PRODUCT_OVERVIEW.md)
>
> **This document is not legal advice.** The GDPR / DDG / TDDDG sections below are a structured, researched starting point written by a developer, not a lawyer.
> Before the site goes public under `event-junkie.de`, have the imprint and the privacy policy reviewed by someone qualified — the cost of a review is far below
> the cost of an *Abmahnung*.

---

## 1. Scope

This plan covers one visible feature — a site footer — plus everything that footer has to link to, because a footer with dead links is worse than no footer:

1. The footer itself (content, layout, component structure).
2. A **version + commit hash** display, sourced from the build rather than typed by hand.
3. A **beta** marker in the header.
4. The **legal pages** the footer links to: imprint, privacy policy, disclaimer.
5. **Third-party licence notices** and a dependency licence policy.
6. **CONTRIBUTING.md**, `CODE_OF_CONDUCT.md`, issue templates and the surrounding community files.
7. **Accessibility** (§12) — pulled in because two of the gaps are Level A failures in the very chrome this plan touches, and because the footer adds another
   block of repeated content to tab through.

It closes six open items in [TODO.md §Legal / Compliance](../TODO.md): imprint, GDPR, FOSS attributions, the GitHub link, and (partially) the accessibility
review and the go-live checklist.

**Explicitly out of scope**, but decided here because they change how the in-scope work is built: full English/German localisation (§6.2, the agreed immediate
follow-up) and donations (§8.4, possible later). Both are recorded as constraints, not as work items.

**Decisions are recorded inline** in the section they belong to, each dated. §13 lists them all; §11 Phase 0 tracks what remains open.

---

## 2. What usually goes in a footer

Surveying what comparable sites actually ship, footer content falls into five groups. Not all of them apply here — the app has no accounts, no payments and no
company behind it.

| Group                   | Typical items                                                                        | Applies to Event Junkie?                                                                                                      |
|-------------------------|--------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| **Legal**               | Imprint, privacy policy, terms, cookie settings, accessibility statement, disclaimer | Yes — imprint, privacy, disclaimer. No cookie banner needed (§7.4). Accessibility statement: later.                           |
| **Identity**            | Copyright line, logo, tagline, licence                                               | Yes                                                                                                                           |
| **Navigation**          | Duplicate of the main nav, sitemap, "popular" links (SEO value)                      | Partly — the nav is four items; a duplicate adds little. A small set of *deep* links (genres, districts) has SEO value later. |
| **Trust / provenance**  | "About", data sources, status page, contact                                          | Yes — About, data sources, contact                                                                                            |
| **Community / product** | Repo link, issues, contributing, changelog, version, social                          | Yes — this is a public FOSS project; this group is the differentiator                                                         |

Things deliberately **not** in the footer: newsletter signup, social icons (no accounts exist), a cookie-settings link (no consent is collected), "back to top"
(the pages are not long enough to earn it). A **language switcher** is out of scope for *this* iteration but explicitly not out of scope for long — full
English/German localisation is the agreed follow-up (§6.2), and the bottom bar is where the switcher will go. Leave room for it.

### Proposed footer content

Three columns on desktop, stacked on mobile, with a bottom bar:

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  ✦ Event Junkie                Project                    Legal                   │
│  Can't get enough of Berlin    Source on GitHub            Imprint                │
│                                Report an issue             Privacy                │
│  Event data is aggregated      Contributing                Open-source notices    │
│  from public sources and       Changelog                                          │
│  provided without warranty                                                        │
│  — always check with the                                                          │
│  venue before you go.                                                             │
├──────────────────────────────────────────────────────────────────────────────────┤
│  © 2026 Event Junkie · Code under Apache-2.0        v0.1.0 · a1b2c3d              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

Notes on the choices:

- The **disclaimer sits in the footer body**, not only on a legal page. It is the single most useful sentence for a user of an aggregator, and nobody clicks
  through to read it.
- **"Report an issue"** points at a GitHub issue template (§10 item 1), not the generic issue list. Making "this event is wrong" a one-click path is a
  data-quality feature disguised as a footer link.
- **"Data sources"** could be added to the *Project* column once a public sources page exists (see §10).

---

## 3. Copyright and licence line

**Is it common? Yes — and it makes sense here, but the wording needs care.**

The subtlety: a footer that says `© 2026 Event Junkie · Apache-2.0` implies the *whole page* — including the event data — is offered under Apache-2.0. It is
not. The event data is scraped from third-party sites; we have no right to sub-licence it, and claiming otherwise is a worse legal exposure than saying nothing.

**Recommendation** — two clauses, unambiguous about what each covers:

```
© 2026 Event Junkie · Code under Apache-2.0 (link → repo)
```

- `© 2026 Event Junkie` — the standard notice for the site's own design and text. A year *range* (`2025–2026`) is unnecessary; a single current year is normal
  and requires no annual edit if generated from the build year, but a hardcoded year is honest too. Recommendation: hardcode the launch year and update it when
  it matters; do **not** use `new Date().getFullYear()`, which produces a wrong notice for archived pages and creates a snapshot-test dependency on the clock.
- `Code under Apache-2.0` — links to `LICENSE` in the repo. This is the GitHub link, so a *separate* repo link in the footer body is redundant. Keep the
  "Source on GitHub" link in the Project column anyway; it is the one people look for and the licence line is not where they look.
- Event data provenance is handled by the disclaimer sentence and, later, a data-sources page.

The header already links to the repo (`App.vue`, `REPOSITORY_URL`). Extract that constant to a shared module (§11) so the header and footer cannot drift.

---

## 4. Version + short commit hash

### 4.1 The question behind the question

A version in the footer serves exactly one purpose: **when someone reports a bug, you need to know what they were running.** That makes "what is deployed right
now" the only correct value — not "what is the newest release on GitHub".

This settles the *"or would it be better to show the release version from the GitHub Releases page to make it 100 % consistent?"* question:

**No — do not fetch the version from the GitHub Releases API.** Reasons, in order of weight:

1. **It answers the wrong question.** The GitHub API tells you the latest *published* release. If a deploy failed, or a rollback happened, or staging is a
   commit ahead, the footer would confidently lie. The build-stamped value cannot lie — it *is* the running artifact.
2. **It is a privacy problem.** A browser call to `api.github.com` transmits every visitor's IP address and User-Agent to GitHub (a US company) on every page
   load. That is a third-country transfer that must then be declared in the privacy policy, needs a legal basis, and undermines the "no third-party requests"
   posture that makes the rest of §7 short and clean. Not worth it for a version string.
3. **Rate limits.** Unauthenticated GitHub API: 60 requests/hour *per IP*. Behind a corporate NAT or a mobile carrier CGNAT, the footer breaks for everyone.
4. **Extra failure mode** for a decorative element.

Consistency is achieved a different way: **the release process makes tag == version.** Tag `v0.1.0` → the build stamps `0.1.0` → the footer links to
`/releases/tag/v0.1.0`. They agree by construction, with no runtime coupling.

### 4.2 Single source of truth: `gradle.properties`

Today the version lives in `build.gradle.kts`:

```kotlin
subprojects {
    group = "de.norm"
    version = "0.0.1-SNAPSHOT"
}
```

Move it to `gradle.properties`, matching the file's existing "centralized versions" convention:

```properties
# Application version. Single source of truth: the Gradle build stamps it into the BFF's
# build-info.properties, the BFF serves it at GET /meta, and the frontend footer displays it.
# Release process: tag `v<version>` == this value, so the footer's release link always resolves.
# Carries -SNAPSHOT on main; the release build overrides it with the tag (see §4.7).
version=0.1.0-SNAPSHOT
```

Gradle sets `version` from a root `gradle.properties` on every project, so the `subprojects { version = ... }` assignment is then **removed** — leaving it in
place would silently override the property. Verify with `./gradlew properties | grep version` and `./gradlew :events-bff:properties | grep version` after the
change.

> **Watch out** — this is the same class of hazard the file already documents for BOM overrides: `version` is a *built-in* Gradle project property, so any
> remaining explicit assignment wins silently and the build stays green while the footer shows the wrong number.

### 4.3 Getting version + commit into the BFF artifact

Two pieces of Spring Boot machinery:

**(a) `build-info.properties`** — add to `events-bff/build.gradle.kts`:

```kotlin
springBoot {
    buildInfo {
        properties {
            // The build timestamp changes on every build, which makes the task perpetually
            // out of date and breaks reproducible builds. The commit hash already identifies
            // the artifact; if a build time is wanted, set it from a CI-provided value instead.
            time = null
            // The FULL sha is stamped; the short form is derived when rendering, so the
            // commit link and `git log` searches both work from one value (see §4.5).
            additional.put(
                "commit",
                providers.exec { commandLine("git", "rev-parse", "HEAD") }
                    .standardOutput.asText.map { it.trim() }
            )
        }
    }
}
```

This generates `META-INF/build-info.properties` inside the jar, which auto-configures a `BuildProperties` bean.

**(b) The commit hash.** The common answer is the `com.gorylenko.gradle-git-properties` plugin (generates `git.properties` → auto-configures a `GitProperties`
bean → `/actuator/info` shows git details). **Recommendation: don't add the plugin.** Use the `providers.exec` snippet above instead, because:

- This project runs with **`org.gradle.configuration-cache=true`** (`gradle.properties`). `providers.exec` is a configuration-cache-safe provider; a plugin that
  shells out to git at configuration time is exactly the pattern that breaks it — and the repo has already paid that tax once with `dependencyCheckAggregate`.
- One additional property is all that is needed. `git.properties` carries ~15 fields, of which we display one.
- Fewer plugins on the build classpath is consistent with the deliberate `skipConfigurations` narrowing in the root build.

Two edge cases to handle in CI:

- `actions/checkout` defaults to a shallow clone; `git rev-parse HEAD` still works, so no `fetch-depth: 0` is needed.
- If `git` is unavailable (source tarball build), the exec fails the build. Wrap it so it falls back to `"unknown"` — or prefer `System.getenv("GITHUB_SHA")`
  when set, falling back to `git rev-parse`.

### 4.4 Exposing it: `/actuator/info` internally, `GET /meta` publicly

**Does Spring Actuator support this? Yes** — `/actuator/info` picks up `BuildProperties` and `GitProperties` automatically via `BuildInfoContributor` /
`GitInfoContributor`, with no code at all. **Both surfaces are in scope**, and they serve different consumers:

|                       | `/actuator/info`                                                                                            | `GET /meta`                                                         |
|-----------------------|-------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| Consumer              | Operators, deployment verification, future monitoring/dashboards                                            | The browser (footer)                                                |
| Reachability          | Internal only — never routed through the public ingress                                                     | Public, via `/api/meta`                                             |
| Payload               | Framework-defined, verbose (`build.group`, `build.artifact`, `build.name`, `build.version`, `build.commit`) | Curated: `version`, `commit`, `commitShort`, and `buildTime` if set |
| Contract              | Owned by Spring Boot; may change across Boot versions                                                       | Ours, versioned with the OpenAPI spec                               |
| Typed in the frontend | No                                                                                                          | Yes, via `schema.d.ts`                                              |
| Cost                  | Configuration only                                                                                          | ~40 lines + a test                                                  |

The reasons `/actuator/info` alone is not enough for the footer are unchanged, and they are what keeps `/meta` in the plan:

- The `info` endpoint is not web-exposed by default; publishing it to the browser means routing the actuator base path through the public ingress, and ADR-012
  explicitly keeps admin surfaces private. Exposing *one* actuator endpoint publicly also makes the ingress rule a per-endpoint allowlist that has to stay
  correct forever — a standing footgun for a decorative feature.
- Its payload shape is framework-defined and leaks `group`, `artifact` and `name` — internal identifiers with no business in a public API.
- It does not appear in the OpenAPI spec, so the frontend's generated `src/api/schema.d.ts` would not type it, breaking the project's typed-client convention.

So: **`/actuator/info` for operators, `GET /meta` for the browser.** Both read the same `BuildProperties` bean, so they cannot disagree.

#### Actuator configuration

Add to `events-bff/src/main/resources/application.yaml` (and mirror it in `events-importer`, which has the same Actuator dependency and benefits equally from
"which build is this importer running?"):

```yaml
management:
    endpoints:
        web:
            exposure:
                # `info` joins the default `health`. This must NOT be routed through the public ingress —
                # the browser gets GET /meta instead (see §4.4). Once the k8s deployment exists, move the
                # management endpoints to a separate port (management.server.port) so the split is enforced
                # by the network rather than by an ingress rule.
                include: health,info
    info:
        build:
            enabled: true
        git:
            # `full` would require a git.properties file, which we deliberately do not generate;
            # the commit hash arrives through build-info.properties as `build.commit` instead.
            enabled: false
```

Notes:

- `management.info.build.enabled` and `management.info.git.enabled` default to `true`, but the contributors only produce output when the corresponding file is
  on the classpath. They are set explicitly here because the *absence* of git properties is a deliberate decision (§4.3) that a future reader will otherwise try
  to "fix".
- `env`, `java` and `os` contributors stay disabled (Boot's default). Do not enable `env` — it exposes configuration properties.
- The importer only needs the config change; it needs no `/meta` endpoint, since nothing user-facing reads it.
- Once a Helm chart exists, prefer `management.server.port: 8081` (BFF) over relying on ingress path rules. Network-level separation is the only version of this
  that cannot be misconfigured later.

#### The public endpoint

```kotlin
// events-bff/src/main/kotlin/de/norm/events/meta/MetaController.kt
@RestController
@RequestMapping("/meta")
class MetaController(private val buildProperties: ObjectProvider<BuildProperties>) {
    @GetMapping
    fun meta(): MetaResponse = MetaResponse.from(buildProperties.ifAvailable)
}
```

- `ObjectProvider` is deliberate: `BuildProperties` **does not exist** when running from the IDE or via `bootRun` without the `bootBuildInfo` task, and a hard
  dependency would fail context startup for every developer. Fall back to `version = "dev"`, `commit = null`.
- Response DTO with `@Schema` annotations, `fromDomain`-style factory — per the conventions in AGENTS.md.
- New Spring Modulith module `meta` under `de.norm.events` — check `ModularityTests` still passes.
- Frontend calls it at `/api/meta` (the Vite proxy and the ingress both strip `/api`). Regenerate the typed client: `npm run generate:api`.

Fetched once on app mount and cached in a composable (`useAppMeta`), not per route. The footer renders nothing (or just the copyright line) until it resolves —
a missing version must never produce a layout shift or an error state.

### 4.5 What the footer renders

```
v0.1.0 · a1b2c3d
   │        └── link → https://github.com/enorm-labs/event-checker/commit/<full-sha>
   └── link → https://github.com/enorm-labs/event-checker/releases/tag/v0.1.0
```

- Serve the **full** SHA from the API and display the short form; the commit link then works without string gymnastics, and `git log` searches are exact.
- In development, `dev` renders as plain text, unlinked.
- A `-SNAPSHOT` suffix renders as-is but unlinked (no such release tag exists).
- `<span class="font-mono">` for the hash; `title` attribute with the full SHA and build time if available.

### 4.6 Frontend version — mirrored in `package.json`, but not displayed

The frontend carries its own `package.json` version and its own commit. Showing two versions in a footer is noise. Frontend and backend are released together
from one repo and one tag, so **one version, from the BFF, is the honest representation**. If the two ever diverge in deployment, revisit — the fix then is to
bake the frontend commit in via a Vite `define` and show it only when it differs.

That is about what the footer *renders*. Separately, `package.json`'s own `version` field mirrors the Gradle version as repository metadata — see the decision
below.

#### Keeping `package.json` in step with Gradle — decision: manual sync (2026-08-07)

**Decision: `events-frontend/package.json` carries the same version as `gradle.properties`, kept in step by hand.** No build-time generation, no Gradle task
writing into `package.json`, no CI check — just a documented convention.

The alternatives that were considered and set aside:

| Approach                             | Mechanism                                                                                                    | Why not                                                                                                                |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| Vite reads the version at build time | `vite.config.ts` resolves `APP_VERSION` → falls back to parsing `../gradle.properties`, exposed via `define` | More machinery than the problem warrants right now; also needs the Docker build context to include `gradle.properties` |
| Gradle writes into `package.json`    | A Gradle task runs `npm pkg set version=…`                                                                   | Mutates a tracked file on every build → dirty working trees, version-bump noise in diffs                               |
| CI check                             | A step fails when the two diverge                                                                            | Available later if drift becomes a real annoyance; deliberately deferred                                               |

**The honest caveat, recorded once:** two files holding the same number will drift, and the drift is silent — nothing breaks, the build stays green, and the
frontend simply carries a stale number. The mitigations below are what keep that cheap, and if it turns out to be annoying in practice, the CI check above is a
one-line escalation that needs no rework of anything else.

**What limits the damage:** `package.json`'s version has **no runtime consumer**. The footer displays the value from `GET /meta` (§4.4), which is stamped from
the *Gradle* build, so a stale `package.json` never produces a wrong version on screen. It is metadata for humans reading the repo — which is exactly why manual
upkeep is proportionate here.

**Mitigations, all cheap:**

- **A pointer comment in both files**, so whoever edits one sees the other. `package.json` has no comment syntax, so use a sibling key:

  ```jsonc
  // events-frontend/package.json
  "version": "0.1.0",
  "//version": "Keep in step with `version` in the root gradle.properties — see docs/FOOTER_AND_LEGAL_PLAN.md §4.6",
  ```

  ```properties
  # gradle.properties — mirror any change here into events-frontend/package.json ("version").
  version=0.1.0-SNAPSHOT
  ```

- **`-SNAPSHOT` is dropped in `package.json`.** npm requires strict SemVer, and `0.1.0-SNAPSHOT` is a valid prerelease string but a misleading one in an
  ecosystem that writes `0.1.0-alpha.1`. Carry the plain number (`0.1.0`) and treat `main` as "the version being worked toward". This means the two files are
  *intentionally* not byte-identical — document that, or someone will "fix" it.
- **Release checklist entry**: bumping the version is one step that touches two files. Put it in `CONTRIBUTING.md`'s release section (§11, Phase 6).

##### Documentation to change

- **`AGENTS.md`** (root) — under **Code Conventions**: the version lives in `gradle.properties`; `events-frontend/package.json` mirrors it without the
  `-SNAPSHOT` suffix; both move together in one commit; the displayed version always comes from `GET /meta`, never from `package.json`.
- **`events-frontend/AGENTS.md`** — the frontend-side half of the same rule. This file matters more than the root one for this purpose, since an agent working
  only in `events-frontend/` loads it and may never read the root file's Gradle conventions. State plainly: **do not bump `version` in `package.json` on its
  own**, and do not "fix" the missing `-SNAPSHOT`.
- **`CONTRIBUTING.md`** (new in Phase 6) — the release steps, listing both files.
- **`README.md`** — only if a version badge is added; it would become a third place to update, so prefer a badge that reads from the latest GitHub release
  rather than a hardcoded one.

### 4.7 Versioning scheme — decision (2026-08-07)

**First public version: `0.1.0`.** Working version on `main`: **`0.1.0-SNAPSHOT`** (replacing today's `0.0.1-SNAPSHOT`).

- **SemVer, starting in `0.x`.** `0.x` states that the public surface is still unstable, which is exactly the claim the beta badge (§5) makes in the header. The
  two belong together: **cutting `1.0.0` and dropping the beta badge should be one decision, not two.**
- **Keep `-SNAPSHOT` on `main`.** It is the Gradle/Maven convention, and it makes "is this a release build?" answerable from the footer alone. §4.5 already
  renders a `-SNAPSHOT` version unlinked, since no matching release tag exists — which is honest rather than a limitation.
- **The release build overrides it from the tag.** When the release workflow is built (it does not exist yet — there is no CD, ADR-012 is still Proposed), it
  should pass `-Pversion=<tag without the leading v>` rather than editing `gradle.properties` on every release. That keeps `main` permanently on `-SNAPSHOT`,
  keeps tag == version by construction, and avoids a release commit whose only content is a version bump.
- **Until that workflow exists**, every build is a `-SNAPSHOT` build and the footer will say so. That is accurate: nothing is released yet. Do not hand-edit the
  property to fake a release number.
- **When to move to `0.2.0`:** on any release that adds user-visible features. Patch releases (`0.1.1`) for fixes. In `0.x`, SemVer permits breaking changes in
  minor bumps — relevant here, because the database schema is explicitly still consolidating into `V001` (see README §Project Status).

---

## 5. Header — the "beta" marker

**Recommendation: a small badge to the right of the logo, linking to the About page.**

```
✦ Event Junkie [beta]   Events  Venues  Calendar  About        [GH] [☾]
```

- Reuse `BaseBadge.vue`; do not introduce a new component.
- Make it a `<RouterLink to="/about#beta">` rather than a tooltip-only element. A tooltip is invisible on touch devices, and `title` alone is not reliably
  exposed by screen readers. Follow the existing pattern in `App.vue`: `title` for the hover tooltip *and* `aria-label` for the accessible name, kept in one
  `computed` so they cannot drift.
- Tooltip text: *"Event Junkie is in beta — data may be incomplete or out of date. See what that means."*
- The About page gains a `#beta` section explaining, in the brand voice: what beta means here (coverage is incomplete, some events are wrong, things change
  without notice), what it does **not** mean (no data is sold, nothing is tracked), and how to help (report an issue). Link on from there to Releases.
- **Watch the header-overflow guard.** `App.vue` documents that all seven items already overflow a ~390 px viewport, and `e2e/smoke.spec.ts` asserts it. The
  badge is an eighth item — verify the wrap behaviour, and consider hiding the badge below `sm` (the About page still carries the information).

Changelog: GitHub's auto-generated release notes are already configured (`.github/release.yml`), so **Releases is the changelog** — do not hand-maintain a
`CHANGELOG.md` in parallel. The footer's "Changelog" link points at `/releases`.

---

## 6. Page structure for the legal content

**Recommendation: three routes, not one and not five.**

| Route            | Title               | Contents                                                               |
|------------------|---------------------|------------------------------------------------------------------------|
| `/legal/imprint` | Imprint             | § 5 DDG provider identification + the disclaimer + liability for links |
| `/legal/privacy` | Privacy             | Full Art. 13/14 GDPR privacy notice                                    |
| `/legal/notices` | Open-source notices | Generated third-party licence list                                     |

Rationale for the split:

- **The notices page is generated and long** (hundreds of entries once transitive dependencies are included). It must not be pasted into a hand-written page —
  keep it a separate route rendering generated data.
- **The disclaimer is three sentences.** It does not deserve a route; German imprints conventionally carry *Haftungsausschluss* alongside the provider
  identification, so put it there and keep the footer's inline sentence as the version people actually read.
- **A separate "Terms of Service" is not needed.** There are no accounts, no payments, no user-generated content, no contract. A ToS would be ceremony.

Routing details:

- Nest under `/legal/*` so a future addition (accessibility statement, data sources) has an obvious home.
- Add `meta: { title: … }` to each route — `usePageTitle` and `e2e/page-title.spec.ts` depend on it.
- Lazy-load all three (`component: () => import(...)`), matching every non-home route.
- These pages must be **prerendered or at minimum crawlable**. German authorities and courts expect an imprint reachable in two clicks from every page; a
  client-rendered SPA route satisfies "reachable" for humans, but consider prerendering before go-live (already an open item in the SEO backlog).

### 6.1 Language — decision: English first (2026-08-07)

The site's UI is English; the legal venue is Germany and the audience is Berlin (heavily international).

**Decision: ship the legal pages in English only, and let German arrive with the localisation work (§6.2) — not before.**

> **This reverses the earlier recommendation in this document** ("German authoritative, English as a convenience translation"). That version was written before
> localisation was agreed as the immediate follow-up, and it leaned on an argument that does not hold up: *"German authorities and any Abmahnung proceed in
> German"* conflates **the language a court proceeds in** with **the language a website must address its users in**. Those are different questions. A German
> court accepts an English document, with a translation where one is needed; nothing about German venue requires German web copy.

The reasoning that actually decides it:

1. **The legal test is comprehensibility for the audience you address, not German per se.** Art. 12 (1) GDPR requires information "in a concise, transparent,
   intelligible and easily accessible form, using clear and plain language". Measured against an all-English site, an **English notice is the better Art. 12
   outcome** — and a German-only notice bolted onto an English UI is arguably the *worse* one, because the people actually reading the site cannot read it. The
   same logic governs the imprint: it must be *leicht erkennbar, unmittelbar erreichbar und ständig verfügbar*, judged against the audience addressed.
2. **Matching the site is itself a coherence argument.** Today every page is English. A single German page in the footer looks like boilerplate copied from a
   generator — which is exactly the impression a legal page should not give.
3. **The copy is about to churn, and duplicated copy drifts.** The privacy notice cannot be final until ADR-012 moves from Proposed to Accepted, log retention
   is decided (§7.5), and the processors are actually contracted. Maintaining two language versions *through* that churn doubles the drift risk — and a stale
   German version is a legal defect, not a cosmetic one. Write once, stabilise, then translate.
4. **It avoids building a toggle that gets deleted** (§6.2 already argues this).
5. **German costs almost nothing when it arrives**, because by then the legal pages are just two more localised routes with stabilised source text.

**Three conditions attach to this decision** — without them, "English first" quietly becomes "English forever":

- **German legal pages ship in the same release as German UI, not after.** This is a hard coupling, not a follow-up ticket. An English-only imprint and privacy
  notice on a site that presents itself in German to a German visitor is indefensible — that is the configuration where the Art. 12 argument above turns against
  us. Put it in the §6.2 work's definition of done.
- **When both versions exist, German becomes the authoritative one**, stated in one sentence on each page ("In case of discrepancies, the German version
  prevails"). The controller, the venue, and the supervisory authority are all German; that is where the earlier instinct was right.
- **Write the English copy as source text for translation** (§6.2): plain sentences, no idiom that has to be re-invented in German. §7.6 already drafts both
  wordings of the disclaimer for exactly this reason.

Consequence for the rest of the plan: **no German text enters the codebase in this iteration.** An earlier draft called for an `AGENTS.md` exception permitting
German on the legal pages; that is no longer needed and has been dropped from §11's documentation list. Fold it into the localisation work instead.

### 6.2 Note — full site localisation (English + German) is the planned follow-up

**Decision recorded 2026-08-07: after this plan ships, the whole site gets localised into English and German.** That is a separate piece of work with its own
plan (and probably its own ADR — library choice, URL strategy and translated-vs-source-language content are all one-way doors), but it changes how the legal
pages should be built *now*, so it is noted here rather than deferred silently.

**What this means for the work in this plan:**

- **Do not build a bespoke language toggle for the legal pages.** An earlier draft proposed one; §6.1 has since settled on English-only precisely so that no
  interim toggle is built. The legal pages ship in one language and become among the first pages the real locale switcher serves.
- **Write footer and legal copy as if it were already extracted.** No sentences assembled from concatenated fragments, no text baked into class names or
  `title`/`aria-label` attributes that a translator cannot reach. The `App.vue` pattern of deriving `title` and `aria-label` from a single `computed` is exactly
  right and should carry over to the footer — it gives one string per concept to translate instead of two that can drift.
- **The disclaimer sentence is a translation unit, not a legal constant.** The English and German wordings in §7.6 are not literal renderings of each other;
  both are already drafted there, so translate from intent rather than word-for-word.
- **Whatever the locale switcher becomes, the footer is where it goes.** §2 excludes it from *this* iteration only; the bottom bar is its conventional home.
  Leave room in the layout rather than packing the bar tight.

**Things to settle in the l10n plan itself** (listed so they are not rediscovered later):

- **Library**: `vue-i18n` is the default choice for Vue 3 and integrates with `vue-router`. Verify Vue 3.5 / Vite 8 compatibility and whether the message
  compiler needs `@intlify/unplugin-vue-i18n` in the build.
- **URL strategy**: prefixed paths (`/de/events`, `/en/events`) versus a cookie/`localStorage` preference. **Prefixed paths are strongly preferred** — they give
  each language a crawlable URL, make `hreflang` possible, and keep the site shareable. A locale kept only in storage is invisible to search engines and to
  anyone receiving a link. Note the interaction with §7.4: a stored locale preference is still "strictly necessary" under § 25 (2) 2 TDDDG, so it does not
  trigger a consent banner — but the URL should be the source of truth and storage only a redirect hint.
- **`<html lang>` is currently `lang=""`** in `events-frontend/index.html` — an empty language attribute. That is an accessibility defect today (screen readers
  fall back to the user's system language and mispronounce content) and a hard blocker for l10n. **Fix it in this plan's Phase 1**: set `lang="en"` statically,
  then make it reactive when locales arrive.
- **Translate the chrome, not the data.** Event titles, venue names, artist names and lineup entries come from the sources and stay in their original language.
  Only UI labels, static copy, legal pages and the empty/error states get translated. Event *type* and *genre* labels are the grey area — they are enum-backed,
  so they are translatable; decide deliberately.
- **Dates and times**: the app already ships `temporal-polyfill`, so use `Intl`-based formatting driven by the active locale rather than any hand-rolled
  formatting in `src/lib/format.ts`. Check that file for hardcoded English month/weekday strings before assuming this is free.
- **SEO**: `hreflang` alternates plus a per-locale `og:locale`. This compounds with the existing prerendering item — a client-rendered SPA with an unresolved
  language is worse for crawlers than one without translations at all.
- **Tests**: `e2e/page-title.spec.ts` and the unit specs assert English strings. Decide early whether e2e runs per locale or pins one, so the suite does not
  have to be rewritten twice.

**Scope note for this plan:** none of the above is in scope here. The only concrete changes it justifies now are the `lang="en"` fix, keeping copy in
translatable shape, and not investing in a legal-pages-only language toggle.

---

## 7. GDPR / DSGVO

### 7.1 What actually applies to this project

Data processed, given the ADR-012 deployment shape (Cloudflare → Hetzner k3s → nginx + BFF → PostgreSQL):

| Data                                                                                     | Where                                                                       | Purpose                                  | Legal basis                                                                |
|------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|------------------------------------------|----------------------------------------------------------------------------|
| IP address, request metadata                                                             | Cloudflare (CDN/WAF) — the only component that certainly sees visitor IPs   | Delivery, DDoS/abuse protection          | Art. 6 (1) (f), plus Art. 28 processor + third-country transfer safeguards |
| Timestamp, request line, status, bytes, referrer, User-Agent — **IP only if we opt in**  | Origin access logs: Traefik ingress + the nginx frontend container (§7.5.1) | Delivering the site; security; debugging | Art. 6 (1) (f) — legitimate interest                                       |
| `theme` preference                                                                       | Visitor's browser `localStorage`                                            | Remembering light/dark choice            | § 25 (2) 2 TDDDG (strictly necessary) + Art. 6 (1) (f)                     |
| **Artist names, lineups and links** — personal data where the artist is a natural person | Our PostgreSQL database; shown on event, artist and venue pages             | Informing the public about events        | Art. 6 (1) (f) — see §7.3, which also requires a removal route             |
| Contact data in a GitHub issue                                                           | GitHub (US)                                                                 | Handling a report                        | Art. 6 (1) (f) — and it is the user's own choice to go there               |

**The Spring applications are not in this table.** The BFF's `RequestLoggingFilter` logs no IP address, and the importer only logs its own outbound scraping —
neither processes visitor personal data. An earlier draft of this table listed `RequestLoggingFilter` as an IP-bearing log; that was wrong, and the corrected
row matters, because it means the *entire* logging question lives in infrastructure that has not been built yet.

No accounts, no analytics, no ad tech, no social plugins, no embedded fonts (Geist is **self-hosted** via `@fontsource-variable/geist` — this avoids the Google
Fonts problem that German courts have already ruled on), no embedded maps, no newsletter, no payment. That is what keeps the notice short.

### 7.2 Mandatory contents of the privacy notice (Art. 13)

A minimal but *complete* notice needs all of these. Missing any one of them is the usual defect:

1. **Identity and contact details of the controller** — name, postal address, email (Art. 13 (1) (a)).
2. **DPO** — *not required here*. Art. 37 GDPR / § 38 BDSG trigger at 20 persons regularly processing, or at core-activity large-scale monitoring. State that
   none is appointed and give the controller's contact instead; do not invent a DPO.
3. **Purposes and legal basis for each processing activity** — the table above (Art. 13 (1) (c)).
4. **The legitimate interests pursued**, spelled out, since Art. 6 (1) (f) is used (Art. 13 (1) (d)): operating and securing the service, defending against
   attacks, diagnosing faults.
5. **Recipients / categories of recipients** (Art. 13 (1) (e)): the hosting provider (Hetzner Online GmbH, Germany, under an Art. 28 AVV) and the CDN/WAF
   provider (Cloudflare — US parent; name the transfer mechanism actually in place, e.g. the EU-US Data Privacy Framework and/or SCCs, and link its DPA).
6. **Third-country transfers** and their safeguards (Art. 13 (1) (f)) — the Cloudflare and GitHub cases.
7. **Retention periods** (Art. 13 (2) (a)) — this needs a *decision*, not a phrase (§7.5).
8. **Data-subject rights** (Art. 13 (2) (b)): access (15), rectification (16), erasure (17), restriction (18), portability (20) and — mandatory to state
   *separately and prominently* because the basis is Art. 6 (1) (f) — **the right to object under Art. 21**.
9. **Right to lodge a complaint** with a supervisory authority (Art. 13 (2) (d)) — for Berlin: *Berliner Beauftragte für Datenschutz und Informationsfreiheit*,
   with address and link.
10. **Whether provision is required** and the consequences of not providing it (Art. 13 (2) (e)) — here: the IP address is technically necessary to deliver the
    page; the site cannot be used without it.
11. **No automated decision-making / profiling** (Art. 13 (2) (f)) — state it explicitly.
12. **Contact route for exercising rights**, and the date/version of the notice.

Things people add that are **not** needed here: a cookie table, consent-withdrawal instructions, Google Analytics/Ads sections, "we may share with partners"
boilerplate. Deleting them is not a shortcut — a notice describing processing that does not happen is itself inaccurate.

### 7.3 Event data is (mostly) not personal data — but artists are people

It is tempting to treat "event data" as wholly non-personal. Careful: **artist names are personal data** where the artist is a natural person, and the schema
stores artists, lineups and links. The processing is almost certainly justified — public information, published by the venue for promotion, processed for
information purposes, Art. 6 (1) (f) with a strong journalistic/public-interest flavour — but it must be **named** in the notice, along with a route for an
artist to request removal. Ignoring it is the most likely real-world complaint this site will receive. The §7.1 table carries a row for it; the notice
additionally needs a short "Artist and event information" section, and the removal route should be one of the `contact_links` in the issue-template config (§10
item 1) so it does not have to be raised in public.

### 7.4 Cookies — and why `localStorage` still matters

The site sets **no cookies**. But the framing "no cookies → nothing to do" is a trap: **§ 25 TDDDG governs the *storage of information on the user's terminal
equipment*, not cookies specifically.** `localStorage` is squarely covered — and `index.html` writes `localStorage['theme']` before first paint.

The good news: § 25 (2) 2 TDDDG exempts storage that is **strictly necessary to provide a service the user has explicitly requested**. A user-set display
preference is a textbook example, so **no consent banner is required**. But it must be *disclosed*: a short "Local storage" section stating what key is set,
what it contains (`light`/`dark`), that it never leaves the device, is not read by any third party, and can be cleared in browser settings.

Keeping this exemption intact is a design constraint worth writing down: **the moment anything non-essential is stored on the device — an analytics ID, a
recently-viewed list used for recommendations, an A/B bucket — a consent banner becomes mandatory** and the whole clean posture collapses. That rule reaches
`AGENTS.md` as part of the standing reminder in §7.7, which covers this case and the rest — do not add it separately.

Recommended policy wording (English, per §6.1; the German rendering follows with localisation): *"This website uses no cookies. Should cookies ever be
introduced, they will be strictly necessary (essential) ones only — no tracking, no analytics tools, no social-media plugins, no advertising trackers."*

### 7.5 Decisions the notice depends on

These are **not** wording questions; the notice cannot be *finally* truthful until they are answered. It can still be written and shipped before then — see the
sequencing note at the end of §7.5.2 — but it cannot be signed off for go-live.

#### 7.5.1 Logging — what is actually to decide

The privacy notice has to state, truthfully, *which* logs hold personal data, *what* is in them, and *how long* they are kept. Right now none of those three has
an answer, so this is four concrete decisions rather than one number.

**Terminology — "origin".** CDN jargon, used throughout this section: the **origin** is everything behind Cloudflare, i.e. the Hetzner box from ADR-012.
Concretely, and this is the whole list of components that could log a visitor IP:

| Component                                      | Logs by default?                            | What it would log                                                                                                             |
|------------------------------------------------|---------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| **Cloudflare** (CDN/WAF)                       | Yes — always, it is the TLS terminator      | The real visitor IP. Not our choice; retention is Cloudflare's, and we name them as processor and link their policy           |
| **Traefik** (k3s ingress)                      | **No** — access logs are off unless enabled | Whatever format is configured, `ClientHost` derived from the peer or from `X-Forwarded-For` if trusted-IP handling is set up  |
| **nginx** (frontend container serving `dist/`) | **Yes** — access log on by default          | `$remote_addr`, which is Cloudflare's proxy IP **unless** the real-IP module is configured to trust Cloudflare and rewrite it |
| **events-bff** / **events-importer**           | Yes, but no IP                              | `METHOD /path?query -> status (Nms)`; the importer logs outbound scraping only                                                |

**You are right that the applications log no IPs**, and that is exactly why this decision is narrow: it is entirely about **Traefik and the nginx frontend
container** — plus Cloudflare, which is not our decision at all. The two Spring apps are out of scope and should stay that way.

The relevant mechanic: **Cloudflare proxies the connection, so the origin's TCP peer is a Cloudflare IP, not the visitor's.** The visitor's real address arrives
only in headers (`CF-Connecting-IP`, `X-Forwarded-For`, `True-Client-IP`), and it lands in a log file only if something is configured to put it there.

**Careful, though — "do nothing" is not a neutral default, because the two components differ:** Traefik's access log is *off* by default (nothing logged), while
nginx's is *on* by default (Cloudflare's IP logged, not the visitor's, until someone adds `set_real_ip_from` + `real_ip_header CF-Connecting-IP`). So the
IP-free outcome is reachable but must be *verified*, not assumed — and the innocuous-looking "restore the real client IP so the logs are useful" change is what
silently flips it.

| # | Decision                                     | Options                                                                                                                                  | Recommendation                                                                  |
|---|----------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| 1 | Do Traefik/nginx log real client IPs at all? | (a) don't restore the real IP — logs carry only Cloudflare's proxy IPs; (b) restore it and log it truncated; (c) restore and log in full | **(a), falling back to (b)**                                                    |
| 2 | **Truncation** of any IP that is logged      | none / IPv4 last octet + IPv6 last 80 bits / hash                                                                                        | **Truncate** if (1) lands on (b)                                                |
| 3 | **Retention** per log stream                 | 3 / 7 / 14 / 30 days                                                                                                                     | **7 days** for access logs                                                      |
| 4 | **Where retention is enforced**              | container runtime rotation, logrotate, k8s, or a log aggregator                                                                          | Decide with the Helm chart; whatever is chosen must be the number in the notice |

**On decision 1 — the interesting one.** The reflex is to restore the real IP "so the logs are useful for debugging". Consider not doing so: rate limiting and
abuse defence happen at Cloudflare, where the real IP is already available in their dashboard. If the origin genuinely does not need visitor IPs, its logs stop
being a meaningful privacy surface, the retention question becomes purely operational, and the notice gets shorter and stronger. Verify against the actual
abuse-handling plan before committing — if origin-side `fail2ban` or per-IP throttling ends up in the design, option (b) with truncation is the answer instead.

**One honest caveat, so the notice does not overclaim.** Even with option (a), origin logs are *lower risk* rather than automatically non-personal: a timestamp
plus request line could in principle be correlated with Cloudflare's records to re-identify a visitor, and Cloudflare is our own processor, so those means are
"reasonably likely to be used" in the sense the CJEU used in *Breyer* (C-582/14). The practical consequence is small — keep describing the logs in the notice
and keep a retention period on them — but do **not** write "our server logs contain no personal data" as an absolute claim.

**On decision 3.** 7 days is the common, well-defended figure for security-purpose logs; 14 is defensible; beyond 30 you are explaining yourself. Pick the
smallest number that still lets you debug an incident reported over a weekend.

**Two rules to carry forward regardless of the outcome:**

- **Never add the client IP to `RequestLoggingFilter`.** It is IP-free today by accident of design; make it deliberate with a comment, so no future "add the IP
  for debugging" change slips through. This is exactly the class of change §7.7's reminder exists to catch.
- **The number in the notice must be the number that is configured.** A stated retention period that rotation does not actually enforce is a worse defect than a
  longer honest one.

#### 7.5.2 The rest

- **Backups.** ADR-012 specifies `wal-g` PITR to a Storage Box. Backup retention is a *separate* retention period from log retention and belongs in the notice
  as its own line. Note that logs kept on disk may or may not be captured by backups depending on the final design — if they are, the effective log retention is
  the backup retention, not the logrotate window. Check this rather than assume it.
- **Art. 28 contracts.** Hetzner's AVV must actually be concluded (their console offers it); Cloudflare's DPA accepted. A notice naming processors without a DPA
  in place is worse than one that names none.
- **Controller identity and address** — decided; see §8.3.

**Sequencing.** None of §7.5.1 blocks Phases 1–2: the pages ship with the privacy notice's infrastructure section written against the ADR-012 *proposed* shape
and marked as such, alongside the address placeholder (§8.3). All of it must be settled and re-verified when the deployment is actually built — which is
precisely the trigger §7.7 puts into `AGENTS.md`.

### 7.6 The disclaimer — *"Alle Angaben ohne Gewähr"* in English

There is no single idiom. Options, by register:

| German                     | English                                                                                    | Register                                        |
|----------------------------|--------------------------------------------------------------------------------------------|-------------------------------------------------|
| *Alle Angaben ohne Gewähr* | "All information without guarantee"                                                        | Literal — reads as translated German; avoid     |
|                            | "All information is provided without warranty as to accuracy, completeness or timeliness." | Formal / legal — **use on the imprint page**    |
|                            | "Subject to change without notice."                                                        | Neutral, common in listings                     |
|                            | "E&OE" (errors and omissions excepted)                                                     | Trade/invoice usage — too obscure for consumers |

**Recommended footer line** (brand voice, and actually useful):

> Event data is aggregated from public sources and provided without warranty — always check with the venue before you go.

**Recommended imprint clause** (formal version):

> **Liability for content.** The event information on this site is aggregated automatically from publicly available sources. It is provided without warranty as
> to accuracy, completeness or timeliness. Events may be moved, sold out or cancelled after we last read the source. Always confirm details with the venue or
> the official ticket seller before travelling. No liability is accepted for decisions taken on the basis of the information shown here.
>
> **Liability for links.** This site links to external websites over whose content we have no control. Responsibility for that content lies with the respective
> operator. The linked pages were checked for legal violations at the time of linking; no such violations were apparent.

### 7.7 Keeping the notice true — a standing reminder in `AGENTS.md`

A privacy notice is not a document you write once. It describes *what the system actually does*, so it silently becomes false the moment the system changes —
and the change that breaks it is rarely labelled "privacy work". ADR-012 is still **Proposed**: the Cloudflare and Hetzner entries in §7.1 describe a deployment
that does not exist yet. If the platform decision moves, the notice ships a factual lie about who processes visitors' IP addresses.

The same applies to features. "Add a map to the venue page", "drop in a Plausible snippet to see what people search for", "embed the venue's Instagram feed",
"remember the last filter the user applied", "add Sentry so we see frontend errors" — every one of these is an ordinary product ticket, and every one changes
the notice, the § 25 TDDDG analysis, or both. An agent implementing such a ticket will not think to check.

**Therefore: add a standing reminder to `AGENTS.md`**, alongside the existing agent instructions. Proposed wording:

> ### Privacy & GDPR — re-check when infrastructure or features change
>
> The public privacy notice (`/legal/privacy`) and imprint describe *what this system actually does*. They are only correct as long as the description matches
> reality, and the changes that break them do not look like privacy work. **Before merging, check whether your change falls into any of the categories below —
> and if it does, say so explicitly in the PR description and update `docs/FOOTER_AND_LEGAL_PLAN.md` §7 plus the privacy page in the same PR.**
>
> **Infrastructure and operations**
>
> - Choosing or changing a hosting provider, CDN, WAF, DNS, mail, backup, or object-storage provider — each is a processor that must be *named*, needs an
>   Art. 28 DPA in place, and, if it is outside the EU/EEA, a transfer mechanism. This is a live question: ADR-012 is still **Proposed**.
> - Changing log content, log retention, or IP handling (truncation/anonymisation) — the notice states a retention period; it must be the real one.
> - Adding monitoring, error tracking, uptime checks, APM, or a metrics backend that receives request or user data.
> - Adding a staging or preview environment that is reachable from the internet.
>
> **Features**
>
> - **Anything stored on the visitor's device** — a cookie, `localStorage`, `sessionStorage`, IndexedDB, or the Cache API. § 25 TDDDG covers *storage on
    > terminal equipment*, not cookies specifically. Today every stored item is strictly necessary, so **no consent banner is required** — that is a property
>   worth protecting deliberately. The first non-essential item (analytics ID, A/B bucket, recommendation history) makes a consent banner mandatory and is a
>   product decision, not an implementation detail. **Escalate rather than implement.**
> - **Any third-party resource loaded by the browser** — a font, script, iframe, map, embed, social widget, or image hotlinked from another host. Each one
>   transmits the visitor's IP address to that host. Fonts are currently self-hosted (`@fontsource-variable/geist`) for exactly this reason; keep it that way.
> - **Any outbound call made from the frontend** to a domain we do not operate (the GitHub API is the tempting one — see §4.1 of the footer plan).
> - **Accounts, login, sessions, newsletter, contact form, comments, favourites, or notifications** — any of these introduces user data we do not currently
>   process at all, and needs its own legal basis, retention period, and deletion route.
> - **New personal data in the domain model.** Artist names are already personal data (§7.3). Adding contact details, social handles, photographs of
>   identifiable people, or user-submitted content extends that materially.
> - **Analytics of any kind**, including self-hosted and "cookieless" tools. Self-hosted and cookieless is a genuinely better posture, but it is still
>   processing and still needs a legal basis and a notice entry.
>
> **Commercial changes** — ads, affiliate links, sponsorships, donations, or paid features also change the § 5 DDG imprint analysis (§8), not just the privacy
> notice.
>
> When in doubt, flag it in the PR rather than deciding silently. The cost of raising it is a sentence; the cost of missing it is a legal defect on a public
> site.

Two supporting measures worth adopting with it, since a reminder nobody reads is worth little:

- **A PR-template checkbox**: *"This change affects data processing / third-party requests / device storage — privacy notice updated."* One line in
  `.github/pull_request_template.md`, and it puts the question in front of every contributor and every agent at the right moment.
- **A dated review anchor.** Put a `Last reviewed: YYYY-MM-DD` line on the privacy page itself and re-check at go-live and whenever ADR-012 moves from Proposed
  to Accepted. A notice with a visible review date is also simply better practice than an undated one.

### 7.8 Note — consider a DSGVO generator later

**Worth evaluating before go-live, not now:** [datenschutz-generator.de](https://datenschutz-generator.de/) (Dr. jur. Thomas Schwenke) is the de-facto standard
German privacy-notice generator. It is modular — you tick the services you actually use and it assembles the corresponding clauses — and it emits German and
English.

**Where it genuinely helps**, and it is not the obvious place:

- **As a cross-check, not a first draft.** Generate a notice from the same inputs as §7.1 and diff it against the hand-written one. Anything the generator
  includes that we omitted is a prompt to check whether we missed a disclosure — which is exactly the failure mode a hand-written notice is prone to (§7.2 lists
  twelve mandatory items; missing one is the usual defect).
- **As a hedge against silent ageing.** Our notice is hand-maintained, so it stays correct only as long as someone remembers (§7.7). A generator with a paid
  update subscription tracks case law and legislative change for you and mails you when a module changes. That is the part worth money — more than the initial
  text.
- **When localisation lands (§6.2)**, it produces both language versions from one set of inputs, which is cheaper and less drift-prone than translating the
  German text by hand.

**Why not lead with it:**

- **Generators over-produce.** They tend to emit clauses for services you do not use, and a notice describing processing that does not happen is itself
  inaccurate — the same defect as omitting one, just in the other direction. Trimming a generated notice down to this project's unusually small footprint is
  more work, and more error-prone, than writing the small thing directly.
- **It does not answer any of §7.5.** Log retention, IP handling and the processor list are our decisions; the generator only formats whatever we tell it.
- **It is not a legal review**, and does not replace the one this document recommends up front.

**Practical terms**, checked 2026-08-07 (re-verify before relying on them):

- **Free tier** for individuals/organisations under €5,000 gross annual revenue — which fits this project today, and most likely still fits once donations
  arrive (§8.4), though donation income does count toward the ceiling. **It requires an attribution backlink to the generator on the privacy page.** That is a
  visible design consequence, not just a licence footnote, so decide it deliberately.
- **Premium: €99.90 net, one-off**, lifetime use plus one year of updates; no attribution requirement; the English version costs extra. Further years of updates
  need a new licence.

**Recommendation:** write the notice by hand as planned (Phase 2), then run the generator as a **pre-go-live cross-check**. Revisit the paid tier at the point
where either localisation or an actual review budget makes it worthwhile — a hand-written notice plus a €99.90 generator plus one hour of a lawyer's time is a
proportionate total spend for a site of this size, and the ordering above gets the most out of each.

---

## 8. Imprint (§ 5 DDG)

**Is it required?** § 5 DDG applies to *geschäftsmäßige, in der Regel gegen Entgelt angebotene* digital services. A **purely private, non-commercial** site is
exempt — but the courts read "commercial" broadly, and the threshold is *low*: a single affiliate link, a banner, a donation button, a sponsor mention, or a
plausible intent to monetise later is enough to cross it. `VISION_ROADMAP_IDEAS.md` contemplates growth beyond a hobby.

**Recommendation: publish an imprint regardless.** It costs one page and removes an entire class of risk. A second consideration points the same way: an
editorially curated events guide can fall under **§ 18 (2) MStV** (journalistic-editorial content), which requires naming a *Verantwortlicher für den Inhalt*
with full address — a stricter test than § 5 DDG and one an aggregator can plausibly meet.

**Required fields** (§ 5 (1) DDG), for a private individual operating without a company:

- Full name (no pseudonym, no "Event Junkie Team")
- **Full postal address** — street and number; a P.O. box is explicitly not sufficient
- Email address, plus a second fast contact route (a contact form counts; a phone number is the safe classic)
- Not applicable here: register/HRB number, VAT ID (§ 5 (1) 6 — only if one exists), supervisory authority, professional title
- If § 18 (2) MStV applies: *Verantwortlich für den Inhalt nach § 18 Abs. 2 MStV:* name and address (may be the same person)
- EU ODR platform link and the § 36 VSBG consumer-arbitration statement: only relevant with consumer contracts — **not needed** for a free information site

### 8.1 The uncomfortable part: this publishes a home address

**First, the bad news: you cannot avoid publishing *an* address by skipping the imprint.** The obvious escape — stay strictly non-commercial, rely on the
private-site exemption, publish no imprint — does not work, because **Art. 13 (1) (a) GDPR independently requires the identity and contact details of the
controller**, and German practice reads that as name *plus postal address*, not an email address alone. The privacy notice is mandatory for every site that
processes personal data, and a site with server logs always does. So the address requirement arrives through the privacy notice even on a purely private,
non-commercial hobby site.

That reframes the question. It is not *"can I avoid publishing an address?"* — it is **"which address do I publish?"**

The legal term of art is a ***ladungsfähige Anschrift*** — an address at which legal documents can be validly served. Crucially, **that is not the same as your
registered residence (*Meldeadresse*)**. Any physical location where post reliably and promptly reaches you can qualify. That is the entire basis of every
option below.

**What definitively does not work**, so it does not get re-litigated later:

- A P.O. box (*Postfach*) or a Packstation — explicitly insufficient.
- An email address, a contact form, or a phone number *instead of* a postal address.
- A pseudonym, a project name, or "Event Junkie Team" in place of a natural person's name.
- Omitting the address and hoping the private-site exemption covers it — see the GDPR point above.

### 8.2 Options, ranked

| Option                                                                 | Cost                                                | Effort                          | Verdict                                                       |
|------------------------------------------------------------------------|-----------------------------------------------------|---------------------------------|---------------------------------------------------------------|
| **A — Impressumsservice / rented *ladungsfähige Anschrift***           | ~€4–25/month                                        | Minutes                         | **Recommended starting point**                                |
| **B — c/o address** (lawyer, tax advisor, family, employer, coworking) | €0 – small                                          | Low, but needs a real agreement | Good if such an address genuinely exists                      |
| **C — Found a UG/GbR** with a business address                         | €300–1,000 setup + running costs, accounting duties | High                            | The honest answer *if* the project monetises                  |
| **D — Stay strictly non-commercial**                                   | €0                                                  | None                            | Reduces exposure; **does not remove the address requirement** |

**Option A — rent an address.** A whole industry exists for exactly this problem. Current market pricing is roughly **€4–7/month at the low end** and
**€20–25/month** for providers bundling scanning, forwarding and a company-registration-capable address. What you are buying is a physical building with a
letterbox bearing your name (or a `c/o` line), plus a defined forwarding process.

Two cautions that matter more than the price:

- **Some providers advertise a *"ladungsfähige Anschrift"* that would not survive scrutiny** — the risk lands on *you*, not the provider, in the form of an
  *Abmahnung*. Check that the address is a real, staffed building, that the mailbox carries your name, and that forwarding is contractually defined with a
  stated turnaround.
- **Forwarding speed is a legal deadline, not a convenience feature.** *Abmahnungen* and court documents carry short response windows; service is effective on
  delivery to the address, not on the day the envelope reaches you. Prefer a provider that scans and notifies same-day over one that batches post weekly, and
  treat "cheapest" as secondary to "fastest".

**Option B — a c/o address.** Legally fine — a `c/o` line is accepted *provided* service of process actually works there. The requirements are concrete: a
physical building, a letterbox labelled with your name or the `c/o` designation, a person who has genuinely agreed to receive post for you, and prompt handover.
A lawyer's or tax advisor's office is the strongest version (they already handle service professionally). A family member's address is the weakest — it works
legally, but it transfers the privacy problem to someone else and makes their address permanently public.

**Option C — found an entity.** A UG or GbR lets you publish a business address, and it is what the project would need anyway once money moves. But it brings
formation cost, accounting obligations, and — for a GbR — no liability shield. **Do not do this for address privacy alone.** Do it when there is revenue, and
take the address benefit as a side effect.

**Option D — stay strictly non-commercial.** Worth stating explicitly because it is a real lever and it interacts with the whole plan: no ads, no affiliate
links, no donations, no sponsorships, no paid features. That keeps the § 5 DDG imprint obligation genuinely arguable and removes the § 18 (2) MStV pressure.
What it does **not** do is remove the Art. 13 GDPR controller-address expectation. Treat it as risk reduction, not as a solution — and note that **it is
time-limited here**: donations are planned for the future (§8.4), so Option D is a temporary posture, not a standing property of the project.

### 8.3 Decision (2026-08-07)

**Option A — a rented *ladungsfähige Anschrift* from [Postflex](https://www.postflex.de/), ordered once `event-junkie.de` is registered.** Option D (stay
strictly non-commercial) stands alongside it while the project is pre-revenue. Rationale unchanged: it costs roughly the price of a coffee per month against a
€30/month hosting budget (ADR-012), it is reversible, it needs no legal formation, and one address serves both the imprint *and* the privacy notice. Move to
Option C only when revenue makes an entity necessary for its own reasons.

Postflex advertises the address explicitly for use in an imprint, and scans incoming post daily to an online portal — which addresses the forwarding-speed
concern in §8.2 (service is effective on delivery to the address, not when the envelope reaches you). Two things to confirm **at ordering time**, because they
are what the §8.2 cautions are about and neither is verifiable from the marketing page:

- The **tariff and its notice period** (pricing is not listed on the landing page — it is behind *"Wähle deinen Tarif"*).
- That the contract covers **use by a natural person for a non-commercial website**, and that the letterbox carries your name or a `c/o` line. Some providers
  scope their product to registered businesses.

Also note for §7.1: Postflex becomes a **recipient of personal data** (it processes post addressed to you), and the address appears on a public page. Neither
changes the privacy notice's *processing* table — this is post, not website data — but the imprint and the notice must carry the same address, and both must be
updated together if the provider ever changes.

#### Sequencing and the placeholder

The address is the **last** thing to land, and until it does the legal pages use an explicit placeholder:

1. Register `event-junkie.de`.
2. Order the Postflex address.
3. Replace the placeholder in the imprint **and** the privacy notice — both, in one commit.

**Placeholder convention** — use a token that is impossible to ship by accident, in both places:

```
Musterstraße 1
12345 Musterstadt
<!-- TODO(imprint-address): placeholder — replace with the Postflex address before go-live. See FOOTER_AND_LEGAL_PLAN.md §8.3. -->
```

Guard it so it cannot reach production silently:

- A **unit test that fails if the string `Musterstadt` appears in any legal view** — cheap, and it turns "we forgot" into a red build. Keep it skipped/inverted
  until go-live if it would block the first PR, but write it in the same PR that introduces the placeholder.
- Add "replace the imprint placeholder" to the go-live checklist in `TODO.md`.
- Because Phase 2 can now proceed without waiting on the address, this is **no longer a blocking decision** — it moves from Phase 0 to the go-live checklist.

**Two related privacy details**, since they leak the same information through a different channel:

- **`.de` WHOIS.** DENIC no longer publishes domain-holder data publicly, so registering `event-junkie.de` does not expose your address by itself. Verify this
  at registration time and avoid any registrar setting that opts into publication.
- **Git history.** `git log` exposes the committer email, which is already `lange.norman@gmail.com` in this repo — no address, so nothing to fix, but worth
  knowing that a public repo makes the identity link explicit regardless of what the imprint says. That is unavoidable for a FOSS project and not a problem in
  itself; it just means anonymity was never on the table, only *address* privacy.

### 8.4 Donations — planned for the future (recorded 2026-08-07)

**Donations or sponsorship may be offered later.** Not in this iteration, but recorded now because it changes the reasoning above rather than merely adding to
it.

**What it changes — and mostly, reassuringly, nothing:**

- **The imprint becomes clearly required**, not merely prudent. § 5 DDG turns on *geschäftsmäßig*, and a donation button is one of the textbook triggers. **This
  costs nothing, because the plan already publishes an imprint (§8.3) with a rented address.** The precautionary decision becomes the load-bearing one — which
  is exactly why it was worth taking early.
- **Option D stops being available** as a fallback posture. Noted above.
- **The BFSG argument weakens.** §10 item 4 rests partly on the site being free and non-commercial. Donations alone probably do not make it a service "against
  payment", but the argument gets thinner; re-check accessibility scope at the point donations go live.
- **The datenschutz-generator free tier has a €5,000 gross annual revenue ceiling** (§7.8). Donation income counts. Unlikely to bite, but it is the kind of
  threshold that is embarrassing to discover retroactively.

**The decision that actually matters when the time comes: link out, never embed.**

| Approach                                                                            | Privacy consequence                                                                                                                                                                                                      |
|-------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **A plain link** to GitHub Sponsors / Ko-fi / Liberapay / PayPal.me                 | The visitor leaves our site. That provider becomes its own controller for whatever happens there. Our notice gains at most a sentence. **Do this.**                                                                      |
| An **embedded widget or button script** (Ko-fi widget, PayPal button JS, Stripe.js) | A third-party resource loaded by the browser on *our* page → transmits every visitor's IP to that provider on page load, needs a notice entry, and may set device storage and therefore require consent under § 25 TDDDG |

The second option would forfeit the "no third-party requests, no consent banner" posture that §7 spends its length protecting — for a button. This is precisely
the case the §7.7 reminder exists to catch, and it is worth deciding now while it is cheap.

**The lowest-friction starting point is the repository, not the website:**
[
`.github/FUNDING.yml`](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/displaying-a-sponsor-button-in-your-repository)
adds a **Sponsor** button to the GitHub repository page, supporting GitHub Sponsors plus external platforms (Ko-fi, Liberapay, Open Collective, Patreon, Buy Me
a Coffee, or up to four arbitrary `custom` URLs). It is a single YAML file and needs no site change at all.

Two distinctions worth keeping straight, because they pull in opposite directions:

- **Privacy: unaffected.** The button renders on `github.com`, not on `event-junkie.de`. No third-party resource loads on our pages, so §7's posture is
  untouched. This is genuinely free from a data-protection standpoint.
- **Commercial character: follows the money, not the button.** Do **not** read "the button is on GitHub, so the website stays non-commercial" as a way to have
  it both ways. If the project accepts donations, § 5 DDG's *geschäftsmäßig* analysis looks at the project, not at which page hosts the link. The imprint is
  published either way (§8.3), so this costs nothing — but the reasoning should not be recorded wrongly.

**Suggested order when the time comes:** `FUNDING.yml` first (zero site impact, real signal), and only then a footer link — still a plain outbound link.

**Two further points to settle before switching donations on** (out of scope for this plan, flagged so they are not discovered late):

- **Pure donation vs. reward-based.** A gift with no consideration creates no consumer contract — so still no terms of service, no *Widerrufsbelehrung*, no § 36
  VSBG statement. Offer *perks* in return ("supporters get X") and it becomes a contract, which drags in all three. Keep donations gift-only unless there is a
  strong reason not to.
- **Tax treatment.** Donations to a private individual are income, and they are not tax-deductible for the donor. Worth one conversation with a *Steuerberater*
  before accepting the first euro — and it is one of the inputs that eventually argues for the UG in §8.2 Option C.

**Footer implication:** a donate link belongs in the *Project* column when it exists. It is explicitly **out of scope for this iteration** — see §10.

---

## 9. Third-party licences and notices

### 9.1 Is ORT the right tool?

[ORT](https://github.com/oss-review-toolkit/ort) is the correct *category* of tool: analyzer (20+ package managers incl. Gradle and npm) → scanner → evaluator
(policy rules) → reporter (NOTICE files, disclosure documents, SPDX/CycloneDX BOMs). It is also **heavyweight**: a Docker image, a config repository with
curations and package configurations, and a scanner step (ScanCode) that takes a long time on a full dependency tree. Its Gradle analyzer has known rough edges
with `build.gradle.kts` in multi-project builds.

**Recommendation: two stages.**

**Stage 1 — lightweight per-ecosystem generation (do this now).** Produces a real notices page in a day, wired into the existing build:

- Backend: `com.github.jk1.dependency-license-report` — a Gradle plugin producing JSON/HTML across all three subprojects, with an `allowed-licenses.json` check
  task that can fail the build.
- Frontend: `license-checker-rseidelsohn` (or `oss-attribution-generator`) as an npm script emitting JSON.
- A small merge step normalises both into one `notices.json`, committed or generated at build time, which `/legal/notices` renders. Grouping by SPDX identifier,
  with the full licence text shown once per licence rather than once per package, keeps the page navigable.

**Stage 2 — ORT (when it earns its keep).** Adopt it when you want *policy enforcement in CI* rather than a list: a `.ort.yml` with rules ("fail on AGPL in a
runtime configuration"), curations for the inevitable packages with wrong metadata, and a proper CycloneDX SBOM for the security story. The
[ORT GitHub Action](https://github.com/oss-review-toolkit/ort) makes this a scheduled workflow, which fits alongside `dependency-check-scheduled.yml`.

Note that the **Apache-2.0 licence of our own code creates an obligation in the other direction too**: § 4 (d) requires that a `NOTICE` file, if the project has
one, be carried into redistributions. If any dependency ships a `NOTICE`, its contents must be reproduced — the generated notices page is where that happens.

### 9.2 Which licences to avoid

The relevant question is not "compatible with Apache-2.0" in the abstract, but **"compatible with a publicly reachable network service whose source is public
under Apache-2.0"**.

| Category                           | Examples                                                 | Verdict                                                                                                                                                                                                                                                                                                       |
|------------------------------------|----------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Permissive                         | MIT, BSD-2/3, Apache-2.0, ISC, Unlicense, Zlib           | ✅ Use freely. Attribution obligations only — which the notices page satisfies.                                                                                                                                                                                                                               |
| Weak copyleft                      | MPL-2.0, EPL-2.0, CDDL, LGPL-2.1/3.0                     | ⚠️ Acceptable for unmodified library use, but each imposes file-level or relinking obligations. Prefer an alternative; if used, record why.                                                                                                                                                                   |
| Strong copyleft                    | GPL-2.0, GPL-3.0                                         | ❌ Avoid. **GPL-2.0-only is outright incompatible with Apache-2.0**; GPL-3.0 is one-directionally compatible (the combined work becomes GPL-3.0), which contradicts the project's licence.                                                                                                                    |
| **Network copyleft**               | **AGPL-3.0**                                             | ❌ **The one to watch.** This project is exactly the trigger case: AGPL's § 13 obligation fires on *network interaction*, not distribution. Our source is public, so compliance is achievable in practice — but it would relicense the combined work and quietly turn an Apache-2.0 project into an AGPL one. |
| Source-available (not open source) | BUSL/BSL, SSPL, Elastic License 2.0, FSL, Commons Clause | ❌ Avoid. Not OSI-approved, use restrictions require legal interpretation, and several forbid exactly "offer this as a service".                                                                                                                                                                              |
| Unknown / missing / custom         | "See LICENSE", no metadata, bespoke terms                | ❌ Treat as a build failure until resolved. This is the largest real category in npm trees and the one automation actually earns its cost on.                                                                                                                                                                 |

Two project-specific notes: **Elasticsearch** appears in the README's future architecture — its licensing history (SSPL/Elastic v2, later AGPL as an option) is
precisely this trap; prefer OpenSearch (Apache-2.0) if that step is taken. And **FullCalendar** (already a dependency) is MIT for the standard packages but its
**premium plugins are commercially licensed** — keep them out.

Recommended CI posture: allowlist by SPDX identifier, fail the build on anything outside it, and require an explicit reviewed exception entry to add one. This
fits the existing `dependency-review.yml` workflow, which can enforce licence rules directly on PRs — the cheapest possible first step, worth enabling before
either tool above.

---

## 10. What else the footer should carry

Ranked by value, with a recommendation for each:

1. **"Report wrong data" / "Suggest a venue"** → GitHub issue templates. **Do it.** This turns the footer into a data-quality funnel, and issue-form templates
   (`.github/ISSUE_TEMPLATE/*.yml`) can require the event URL and venue name up front — see
   [configuring issue templates for your repository](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/configuring-issue-templates-for-your-repository).
   Two things from that doc worth using deliberately: `config.yml` with `blank_issues_enabled: false` forces reporters through a form rather than a blank box,
   and `contact_links` can route anything that should *not* be a public issue (security reports, artist removal requests under §7.3) somewhere else. Templates
   also accept prefilled labels, which keeps the release-notes categories in `.github/release.yml` working without hand-labelling.
2. **Data sources / attribution page.** **Do it, soon.** A page listing which venues are imported, how often, and linking to each source is a transparency and
   SEO asset — and it is the visible half of the "confirm legality of scraping" TODO item. It also gives venues a self-service route to ask for changes.
3. **Contact email.** **Required anyway** by the imprint and the privacy notice. Use a role address, not a personal one.
4. **Accessibility statement.** Later — and it depends on §12: a statement is a *claim*, so it can only be published once the target is actually met and
   measured. Note also: the **BFSG** (in force since 28 June 2025) covers certain B2C services; a free, non-commercial information site is very likely out of
   scope, and micro-enterprises are exempt for services — but the exemption is not automatic once the site is commercial (§8.4). Track it in the go-live
   checklist rather than the footer for now.
5. **Status / uptime page.** Nice-to-have once an external monitor exists (already in the ADR-012 consequences).
6. **SEO deep links** (top genres, districts). Later, and only once those routes exist.
7. **"Built with AI" note.** The README makes this point prominently; a one-line footer nod links to it and is on-brand.

Explicitly **not** recommended *in this iteration*: social icons (no accounts), a newsletter, and a donate link — the last one is **planned for later** (§8.4),
belongs in the *Project* column when it arrives, and must be a plain outbound link rather than an embedded widget. What follows is the original reasoning, which
still holds for why it is not in the first version: it strengthens the commercial reading under § 5 DDG, which should be a deliberate step rather than an
accident. Also not recommended, and this one permanently: a cookie-settings link — there is no consent to collect.

---

## 11. Implementation plan

### Phase 0 — decisions (all resolved except one, which blocks nothing in Phases 1–6)

- [x] ~~Imprint address strategy~~ — **decided 2026-08-07**: rent a *ladungsfähige Anschrift* from [Postflex](https://www.postflex.de/), ordered once
  `event-junkie.de` is registered (§8.3). This no longer blocks anything: the legal pages ship with a guarded placeholder, and swapping in the real address
  moves to the go-live checklist
- [x] ~~Language of the legal pages~~ — **decided 2026-08-07**: English only for this iteration; German arrives with localisation, in the same release as the
  German UI (§6.1)
- [ ] Log retention, IP truncation, and whether **Traefik / the nginx frontend container** log real client IPs at all (§7.5.1 — four sub-decisions, table
  there). The Spring apps are not involved: they log no IPs. **Does not block Phases 1–2**; settle it when the deployment is actually built, since it depends
  entirely on infrastructure that does not exist yet
- [x] ~~First public version number and `-SNAPSHOT` handling~~ — **decided 2026-08-07**: first public version `0.1.0`; `main` carries `0.1.0-SNAPSHOT`; the
  release build overrides the version from the tag; `1.0.0` and dropping the beta badge are one decision (§4.7)

### Phase 1 — footer shell and static links ✅ done (2026-08-07)

Delivered as planned, with four things worth recording because they were not foreseen:

- **The footer's `<nav>` created a second navigation landmark**, which broke every existing
  `getByRole('navigation')` selector. Fixed properly rather than by loosening the selectors: the
  header nav now carries `aria-label="Main"` (multiple landmarks must be distinguishable anyway),
  and the e2e specs address it by name.
- **The skip link targets one `#main-content` wrapper** around `<RouterView />` rather than each
  view's own `<main>` — one target instead of seven to keep in sync.
- **axe found two genuine contrast failures**, both in the FullCalendar bridge in
  `EventCalendar.vue`, and both fixed at the token rather than excluded: `--fc-classic-faint-…`
  dimmed day numbers to 70% alpha, and `--fc-classic-button-strong` mixed toward literal `black`,
  which in dark mode moved the active button's background *toward* its near-black text (3.0:1).
- **`eslint-plugin-vuejs-accessibility` found three pre-existing issues.** Two were an over-strict
  default (`label-has-for` demanding nesting *and* `for`/`id`; implicit association is valid) and
  are configured, with the reasoning recorded in `eslint.config.ts`. The third is real but latent:
  `BaseSelect` takes its accessible name from a fall-through `aria-label`, which nothing enforces.
  Suppressed with a comment and **left as a follow-up** — making the label a required prop changes
  a shared component's API and every call site.

Verified: `type-check`, `lint`, `test:unit` (48), `test:e2e --project=chromium` (90), `build-only`.

**Original scope, for reference:**

New: `events-frontend/src/components/AppFooter.vue`, `src/lib/links.ts` (shared `REPOSITORY_URL`, releases/issues/commit URL builders). Modified: `src/App.vue`
(render `<AppFooter />` after `<RouterView />`; wrap the shell in a flex column so the footer sits at the bottom on short pages; import
`REPOSITORY_URL` from `links.ts` instead of the local constant); `index.html` (`<html lang="en">` — it is currently `lang=""`, a WCAG 3.1.1 Level A failure
today and a blocker for §6.2). Tests: `src/components/__tests__/AppFooter.spec.ts` (renders links, no external requests), `e2e/footer.spec.ts` (footer visible
on every route; imprint reachable in one click; no horizontal overflow at 390 px).

**Accessibility work lands here too** (§12.3), because it is cheapest before the footer adds another block of repeated content:

- The **skip link** in `App.vue` — first focusable element, visually hidden until focused, targeting `<main id="main" tabindex="-1">` (WCAG 2.4.1, Level A).
- `eslint-plugin-vuejs-accessibility` wired into `eslint.config.ts`, so `npm run lint` covers it.
- `@axe-core/playwright` and `e2e/a11y.spec.ts` sweeping every route. Expect this to surface contrast findings on `text-muted-foreground` (§12.2 item 4) —
  budget time to fix the token rather than to silence the rule.
- The accessibility section in `events-frontend/AGENTS.md` (§12.4).

Write all footer copy in translatable shape (§6.2): whole sentences, one string per concept, nothing baked into attributes a translator cannot reach.

### Phase 2 — legal routes and copy ✅ done (2026-08-07)

Delivered as planned. Five decisions taken during implementation that the plan did not anticipate:

- **`src/lib/legal.ts` holds the controller details once**, and both pages render from it. §8.3
  requires the imprint and the privacy notice to carry the same address; sharing a module makes
  that structural rather than a thing to remember, and a unit test asserts it reaches both pages.
- **A visible "this page is not final" banner** (`ProvisionalNotice.vue`), driven by two flags in
  that module. The address is a placeholder and ADR-012 is still *Proposed*, so both pages would
  otherwise assert things that are not true — and an inaccurate notice is the exact defect these
  pages exist to avoid. Both flags must be `false` before go-live.
- **The placeholder tripwire is an invariant, not a skipped test.** Rather than `it.skip`, the test
  asserts that the provisional flag and the placeholder address *agree*. That holds both before and
  after go-live, so it never rots — and it fails if someone swaps in the real address without
  dropping the banner, or drops the banner while the placeholder remains.
- **`/legal/notices` is routed but not linked from the footer.** It has no generated content until
  Phase 5, and Phase 1's "no thin or dead links" discipline applies to a stub page too. Phase 5
  adds the link; a unit test asserts the link is absent so that phase cannot forget.
- **A `scrollBehavior` was added to the router.** Legal links live in the footer, so they are always
  followed from a scrolled position — without it the imprint opened halfway down.

The privacy notice is structured against the twelve Art. 13 elements in §7.2, and the unit suite
walks that checklist item by item — a missing element is invisible without one.

Verified: `type-check`, `lint`, `test:unit` (78), `test:e2e --project=chromium` (101), `build-only`.
The axe sweep now covers all three legal routes.

**Original scope, for reference:**

New: `src/views/legal/ImprintView.vue`, `PrivacyView.vue`, `NoticesView.vue`; routes under `/legal/*` with `meta.title`; a shared `LegalLayout` or a prose
utility class (the existing views use a hand-rolled `mx-auto max-w-3xl` — reuse that rather than adding a typography plugin). Also: the disclaimer sentence in
the footer; the About page's `#beta` section; the `Last reviewed:` date on the privacy page.

The postal address in the imprint **and** in the privacy notice's controller block is the `Musterstraße 1 / 12345 Musterstadt` placeholder from §8.3, with the
`TODO(imprint-address)` comment and the guard test. Everything else on both pages is final copy — the placeholder is the only unfinished element.

**Land the `AGENTS.md` reminder and the PR-template checkbox from §7.7 in this same phase**, not later. The reminder exists to protect a notice that does not
exist yet — but if it goes in after the pages ship, the first change that invalidates them will already have merged.

### Phase 3 — version pipeline ✅ done (2026-08-07)

Delivered as planned. Four departures worth recording:

- **`build.time` is kept, and §4.3's rationale for dropping it was wrong.** The claim was that a
  build timestamp leaves `bootBuildInfo` perpetually out of date. It does not — the timestamp is
  not a task input, and the task was verified UP-TO-DATE across consecutive builds. Separately,
  `properties { time = null }` (what Boot's older docs show) does **not** work on Boot 4: an unset
  `time` falls back to `Instant.now()`; omitting it requires `excludes.add("time")`. Since the only
  remaining argument was reproducible builds — which this project does not require — the timestamp
  stays, and `/meta` exposes it as `buildTime`, which §4.4's response table already anticipated.
- **`buildInfo` is configured once in the root build**, under `plugins.withId("org.springframework.boot")`,
  rather than per-module. The importer needs the same stamping for its `/actuator/info` to be
  anything other than inert, and the git plumbing should exist once.
- **`GITHUB_SHA` is preferred over `git rev-parse`** when set, with `"unknown"` as a final fallback
  so a source-tarball build (no `.git`) does not fail. `MetaResponse` maps `"unknown"` to a null
  commit, so the footer never renders a link to `/commit/unknown`.
- **An e2e test was made deterministic rather than ambient.** "Shows no version line when the
  backend is unreachable" initially relied on no BFF running during e2e — true for CI, false for a
  developer with the stack up, and it failed exactly that way during implementation. It now forces
  the failure with `route.abort()`.

Verified: `./gradlew clean build koverLog` (95.6% line coverage), configuration cache **reused** on
a repeat run with the same task set, `/meta` and `/actuator/info` agreeing on version and commit
from a **packaged** `java -jar` run, and the frontend gate (85 unit, 103 e2e, build).

**Original scope, for reference:**

Modified: `gradle.properties` (`version=…`), `build.gradle.kts` (drop `version` from `subprojects`), `events-bff/build.gradle.kts` (`springBoot { buildInfo }`),
`events-bff/src/main/resources/application.yaml` **and** `events-importer/src/main/resources/application.yaml` (expose `info`, §4.4 — same two lines in both;
the importer gets no `/meta`, since nothing user-facing reads it). New: `events-bff/.../meta/MetaController.kt`, `MetaResponse.kt`, `MetaModule` marker if the
module convention requires one; `MetaControllerTest`. Frontend: `src/composables/useAppMeta.ts`, `package.json` (`version` → `0.1.0`, plus the `//version`
pointer key, §4.6), `npm run generate:api` to refresh `schema.d.ts`, footer version line. Docs: the version-sync rule in `AGENTS.md` **and**
`events-frontend/AGENTS.md` (§4.6).

Verify:

- `./gradlew clean build koverLog` — a new controller must not drop coverage below the threshold; `ModularityTests` must still pass.
- The configuration cache is still reused: `./gradlew build` twice, the second run must report "Configuration cache entry reused". This is the specific risk of
  the `providers.exec` approach in §4.3 and the reason the git-properties plugin was rejected.
- `/actuator/info` returns a populated `build` block from a **packaged** run (`./gradlew :events-bff:bootJar`, then `java -jar`), not only from `bootRun` — the
  `BuildProperties` bean exists only when `build-info.properties` is on the classpath, which is exactly the failure mode `ObjectProvider` hides during
  development.
- `/meta` and `/actuator/info` report the same version and commit — they read the same bean, so a mismatch means one of them is stale or misconfigured.
- `events-frontend/package.json` carries the matching version (§4.6). It has no runtime consumer, so nothing fails if it is wrong — check it by eye, in the same
  commit as the `gradle.properties` change.
- The actuator path is **not** reachable through whatever fronts the app (dev: the Vite proxy only forwards `/api`; production: the ingress rule).

### Phase 4 — beta badge ✅ done (2026-08-07)

Delivered as planned: a `BaseBadge` inside a `RouterLink` to `/about#beta`, with the tooltip and
the accessible name read from one constant — "beta" alone is a useless link name out of context.
The header-overflow guard passes at 390 px with the badge added, so no `sm` breakpoint hiding was
needed.

**Running the full browser matrix for the first time exposed two latent bugs in my own earlier
tests** — Phases 1–3 were verified with `--project=chromium`, as the plan's verification steps
specified, and both failures were invisible there:

- **The skip-link test asserted a Tab press focuses it**, which fails on WebKit and Mobile Safari:
  those exclude links from the Tab order unless macOS Full Keyboard Access is on. That is a
  platform default, not an app defect. Split into a functional test (all browsers, via `.focus()`)
  and a tab-order test that skips on WebKit.
- **The legal-page scroll test used `mouse.wheel`**, which is a no-op on touch-emulating projects —
  so on Mobile Safari the page was never scrolled and the assertion passed **vacuously**. Now uses
  `window.scrollTo` with a poll confirming the scroll actually happened.

The second is the more useful finding: a test that passes for the wrong reason is worse than one
that fails. **Verification for later phases should run the full matrix, not just chromium.**

Verified: `type-check`, `lint`, `test:unit` (85), `test:e2e` across **all five projects**
(523 passed, 2 skipped on WebKit by design), `build-only`.

**Original scope, for reference:**

Modified: `src/App.vue` (badge + header wrap check), `e2e/smoke.spec.ts` (extend the header-overflow guard to the new item).

### Phase 5 — licence notices ✅ done (2026-08-07)

Delivered in the order the plan prescribed: CI deny-list first, then the Stage-1 generators, then
the page. Five things worth recording:

- **CI uses a deny-list, the local check uses an allow-list**, deliberately. `dependency-review`
  sees only *new* dependencies and unnormalised SPDX strings, where an allow-list fails on every
  unrecognised-but-fine spelling and trains people to bypass the gate. `checkLicense` runs over the
  full resolved tree with normalised names, where an unknown licence *should* stop the build.
- **The allow-list matches the normaliser's verbatim names** (`The 2-Clause BSD License`, not
  `BSD-2-Clause`). My first attempt used SPDX identifiers and failed on four perfectly fine
  dependencies. The file says so, so the next person does not repeat it.
- **LGPL-2.1 and CC0 are deliberately absent** from the allow-list. Logback and HdrHistogram are
  dual-licensed and pass on EPL-2.0 and BSD-2 respectively, so keeping those two off means an
  LGPL-only or CC0-only dependency stops the build and gets a decision rather than passing silently.
- **The licence-report plugin is not configuration-cache compatible** — the same defect the repo
  already documents for `dependencyCheckAggregate`, and now documented alongside it. Its tasks take
  `--no-configuration-cache`, and `checkLicense` is a *separate* CI step rather than wired into
  `check`, so the main build's cache entry is not discarded on every run (verified: still reused).
- **Enforcement covers both ecosystems — but only after a correction.** The first cut checked JVM
  dependencies only: `checkLicense` reads `runtimeClasspath` across the Gradle projects, and the
  frontend is not one, so the ~485 npm packages were covered solely by `dependency-review`, which
  sees *newly introduced* dependencies and had therefore never looked at the existing tree. The
  allow-list, written from the backend tree, silently omitted four licences that exist only on the
  npm side (BlueOak-1.0.0, MPL-2.0, CC-BY-4.0, OFL-1.1 — all acceptable, none considered).
  Fixed by `npm run check:licenses`, a frontend counterpart wired into `build-frontend.yml`, and by
  renaming the policy files to `allowed-licenses-jvm.json` / `allowed-licenses-npm.json` so neither
  reads as the whole policy. Two files rather than one because the ecosystems report licence names
  in different vocabularies — SPDX ids vs the Gradle normaliser's prose names.
- **The page states its own scope**, because the generated list is broader than what ships: it
  covers the dependency graph (642 components), and bundling drops much of it. It also records
  licences rather than reproducing full texts or per-package `NOTICE` files. Claiming completeness
  would be the inaccuracy this whole section exists to avoid — full reproduction is the ORT
  (Stage 2) upgrade.

`notices.json` is generated and **committed**: the frontend is not a Gradle subproject, so its
build must not have to invoke Gradle, and the page then works in `npm run dev` with nothing else
run first. The generator writes no timestamp, so regenerating with unchanged dependencies yields an
empty diff.

Verified: `./gradlew clean build koverLog` (95.6%), configuration cache reused on a repeat build,
`checkLicense` green over the full tree, frontend gate (85 unit, **533 e2e across all five
projects**, build). The notices chunk is 77 kB raw / 11.5 kB gzip and lazy-loaded.

**Original scope, for reference:**

`dependency-review.yml` licence rules first (cheapest), then the Stage-1 generators (§9.1), then the `/legal/notices` page rendering the generated data.

### Phase 6 — community files ✅ done (2026-08-07)

Delivered as planned, plus the footer's "Contributing" link, which Phase 1 had deferred to here.
Four things worth recording:

- **The Code of Conduct is the verbatim Contributor Covenant 3.0**, fetched from source rather than
  paraphrased, with only the two adopter placeholders filled — a code of conduct that has been
  reworded is no longer the thing people think they are agreeing to. The CC BY-SA 4.0 attribution
  block is intact, as that licence requires. A script assertion fails if any `[NOTE: …]` placeholder
  survives.
- **A fourth issue form was added ("Something else").** The plan specified `blank_issues_enabled: false`
  plus three forms, which would have left a plain question with nowhere to go. The obvious fix —
  a `contact_links` entry pointing at Discussions — turned out to be a dead link, because
  **Discussions is not enabled on this repository**. A one-textarea form keeps structure the default
  without closing the door.
- **The private-report route is GitHub's security advisory form**, used for both security reports
  *and* artist removal requests. Neither belongs on a public tracker, and it is the only
  confidential channel that exists until `event-junkie.de` is registered. Both the CoC and
  `SECURITY.md` say so plainly rather than listing an address that does not yet receive mail.
- **`SECURITY.md` scopes itself to the code**, because nothing is deployed: it points researchers at
  the scrapers, which parse untrusted HTML from dozens of third-party sites and are by far the
  largest untrusted-input surface here, and it explicitly asks people *not* to test against the
  venues' own websites.

Verified: frontend `type-check`, `lint`, `test:unit` (85), `test:e2e` across all five projects
(533 passed, 2 skipped on WebKit by design), `build`, `check:licenses`; every issue template parses
and every label it applies already exists; all relative links and heading anchors in the new
Markdown resolve.

**Original scope, for reference:**

New: `CONTRIBUTING.md`,
[
`.github/ISSUE_TEMPLATE/`](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/configuring-issue-templates-for-your-repository)
(bug, wrong event data, new venue request, plus `config.yml` with `blank_issues_enabled: false` and `contact_links` routing security and artist-removal requests
off the public tracker), `SECURITY.md`, and `CODE_OF_CONDUCT.md`.

**`CODE_OF_CONDUCT.md` — decided 2026-08-07: adopt [Contributor Covenant 3.0](https://ethicalsource.dev/projects/contributor-covenant-3/).** Version 3.0
(released 28 July 2025) is the current one — do not paste the 2.1 text that most search results still surface. It is the version Django and Mastodon moved to
during 2026, its language is deliberately less US-centric and easier to translate (which matters given §6.2), and its enforcement section is structured as
"Addressing and Repairing Harm" rather than a fixed ladder of punishments. The site provides a builder tool for filling in the project-specific parts.

Three things to get right, because an unenforceable code of conduct is worse than none:

- **The reporting contact must be a real, monitored address** — the same role address the imprint and privacy notice use (§10 item 3), not a personal inbox and
  not "open an issue". A code of conduct whose reporting route is a public issue tracker asks people to report harassment in public.
- **Name who handles reports.** With a single maintainer that is you, and saying so plainly is better than an institutional "the project team" that does not
  exist. Revisit if the project ever gains co-maintainers.
- **Write only the enforcement steps you would actually carry out.** The obligation this creates is real: adopting it and then not acting on a report is a worse
  position than never having adopted it.

GitHub surfaces `CODE_OF_CONDUCT.md` in the community-standards checklist and links it from the issue and PR forms automatically, so no footer link is needed —
`CONTRIBUTING.md` should reference it, and that is enough. See
[adding a code of conduct to your project](https://docs.github.com/en/communities/setting-up-your-project-for-healthy-contributions/adding-a-code-of-conduct-to-your-project)
for the mechanics: the file must sit in the repository root, `docs/`, or `.github/` to be detected, and GitHub's web UI offers a Contributor Covenant template —
**note that the built-in template is version 2.1**, so prefer the 3.0 text and builder linked above over the one GitHub pre-fills.

`CONTRIBUTING.md` should cover,
per [GitHub's guidance](https://docs.github.com/en/communities/setting-up-your-project-for-healthy-contributions/setting-guidelines-for-repository-contributors):
how to report a bug or wrong event data (link the templates), local setup (point at README + `events-frontend/README.md`, don't duplicate), the AI-agent
workflow and `AGENTS.md` as the source of truth for conventions, `./gradlew clean build` + `/verify` before opening a PR, Conventional Commits titles (they
drive `label-pr.yml` and the release notes), the rebase-not-merge rule, how importers are structured for the most likely contribution type, the version-bump
rule touching both `gradle.properties` and `package.json` (§4.6), a link to `CODE_OF_CONDUCT.md`, and the Apache-2.0 inbound=outbound licensing statement.

### Phase 7 (follow-up, separate plan) — localisation

Full English/German localisation of the site, as agreed in §6.2. Not part of this plan's delivery, but it is the immediate next piece of work and it retires
§6.1's interim answer: once locales exist, the legal pages are simply two more localised routes and the "which language is authoritative" question becomes a
statement on the page rather than a routing decision. Expect its own plan document and an ADR covering library, URL strategy, and the translate-the-chrome-not-
the-data boundary.

### Documentation to update alongside

- `TODO.md` — tick the Legal/Compliance items as they land; add the localisation follow-up (§6.2) as a tracked item; add **"register `event-junkie.de` → order
  the Postflex address → replace the imprint/privacy placeholder"** to the go-live checklist as one ordered item (§8.3); add "cross-check the privacy notice
  against a DSGVO generator" to the same checklist (§7.8)
- `AGENTS.md` — **the standing privacy/GDPR re-check reminder (§7.7 — wording is drafted there, ready to paste)**; the version-in-`gradle.properties`
  convention **and the manual `package.json` mirror rule (§4.6)**; the "write UI copy in translatable shape" rule. The §7.7 block supersedes the narrower
  "nothing non-essential in `localStorage`" rule mentioned in §7.4 — it covers that case and the rest. No German-text exception is needed: per §6.1 the codebase
  stays English-only until localisation lands.
- `events-frontend/AGENTS.md` — **the accessibility section (§12.4 — drafted there, ready to paste)**, plus the frontend half of the version-mirror rule (§4.6).
  Easy to overlook, and the more important of the two files: an agent working only inside `events-frontend/` loads this one and never sees the root file's
  Gradle conventions. The accessibility rules matter most here, because everything in §12.1 is currently folk knowledge that survives only as long as the next
  contributor imitates the surrounding code.
- `.github/pull_request_template.md` — the privacy checkbox from §7.7 (the file does not exist yet; creating it is the cheapest half of the reminder)
- `README.md` — link CONTRIBUTING; the badge row may want a version badge
- `BRANDING.md` — the brand voice is currently defined in English only; localisation needs a German register for the tagline and the beta copy, and *"Can't get
  enough of Berlin"* does not translate literally
- Consider an **ADR** for the version-exposure decision (§4) — it is exactly the kind of "we chose the build-stamped value over the GitHub API, and here is why"
  reasoning ADRs exist to preserve

---

## 12. Accessibility (WCAG 2.1)

**Are we already following it? Substantially in practice, but not formally — and there are two Level A gaps.**

The honest summary: the frontend shows real, deliberate accessibility work, well above what a project at this stage usually has. What is missing is not
craftsmanship — it is a *stated target*, *verification*, and anything that stops a regression.

### 12.1 What is already right

Verified in the codebase (2026-08-07), not assumed:

| Practice                                                                                                  | Where                                                  | WCAG criterion                                    |
|-----------------------------------------------------------------------------------------------------------|--------------------------------------------------------|---------------------------------------------------|
| Route-change announcer — `role="status"`, `aria-live="polite"`, `aria-atomic`, visually hidden            | `App.vue`                                              | 4.1.3 Status Messages (AA)                        |
| `aria-label` on every icon-only control, kept in sync with `title` through one `computed`                 | `App.vue` (theme toggle, repo link)                    | 4.1.2 (A), 2.5.3 Label in Name (A)                |
| `aria-label` on all eleven filter inputs; `aria-pressed` on the date-preset toggles                       | `EventFilterBar.vue`, `VenuesView.vue`                 | 4.1.2 (A)                                         |
| `aria-hidden="true"` on decorative SVG marks                                                              | `GitHubMark.vue`, `PulseMark.vue`, `HomeView.vue`      | 1.1.1 Non-text Content (A)                        |
| `alt` on every image                                                                                      | `EventCard.vue`, `VenueCard.vue`, `BaseDetailView.vue` | 1.1.1 (A)                                         |
| A `<main>` landmark on every route (the three detail views inherit it from `BaseDetailView`)              | `views/`, `BaseDetailView.vue`                         | 1.3.1 Info and Relationships (A)                  |
| Visible focus ring — `focus-visible:ring-3` with a dedicated ring colour, including a destructive variant | `components/ui/button/index.ts`                        | 2.4.7 Focus Visible (AA)                          |
| Accessible component primitives (reka-ui, the Vue port of Radix)                                          | `components/ui/`, `shadcn-vue`                         | various                                           |
| e2e tests that query **by role and accessible name**                                                      | `e2e/*.spec.ts`                                        | — a regression in naming already breaks the suite |

That last row is worth calling out: because the Playwright suite addresses elements the way a screen reader does, a chunk of accessibility is *already*
regression-tested as a side effect. That is a better starting position than most projects have.

### 12.2 Confirmed gaps

1. **`<html lang="">` — an outright Level A failure of 3.1.1 Language of Page.** An empty `lang` is worse than a wrong one: screen readers fall back to the
   user's system language and mispronounce the whole page. Already scheduled in **Phase 1** (§6.2).
2. **No skip link — 2.4.1 Bypass Blocks (Level A).** The header nav repeats on every route, and a keyboard or switch user must tab through brand, four nav
   links, the repo button and the theme toggle before reaching content on *every* navigation. The footer this plan adds makes the page longer, not shorter. Fix:
   a visually-hidden-until-focused "Skip to content" link as the first focusable element in `App.vue`, targeting the `<main>` that already exists on every view
   (give it `id="main"` and `tabindex="-1"`).
3. **No automated checking.** No `eslint-plugin-vuejs-accessibility`, no axe in the Playwright run. Everything above was achieved by hand, which means nothing
   prevents the next component from regressing it.
4. **Colour contrast is unverified (1.4.3, AA).** The dark-first palette in `main.css` has never been measured. `text-muted-foreground` on `bg-background` is
   the most likely offender, and it is used in nearly every view — including the footer copy this plan introduces.
5. **FullCalendar is a third-party widget** whose keyboard and screen-reader behaviour we have not assessed. It is lazy-loaded on one route, so it is contained,
   but it should not be assumed conformant.

### 12.3 What to do — decision

**Target: WCAG 2.1 Level AA.** AA is the level German and EU law reference (BFSG/EN 301 549), it is the normal commitment for a public site, and — given §12.1 —
the distance to it is small. Do **not** claim conformance until §12.2 is closed and something has actually measured it.

- **Close the two Level A gaps in Phase 1**, alongside the footer: `lang="en"` and the skip link. Both are a few lines, and both are cheapest before the footer
  adds another block of repeated content.
- **Add automated checking in Phase 1 as well** — `eslint-plugin-vuejs-accessibility` (catches missing labels, redundant roles and bad `alt` at lint time, in
  the existing `npm run lint`) and `@axe-core/playwright` as one `e2e/a11y.spec.ts` sweeping every route. Neither is a large dependency, and axe will settle the
  contrast question in §12.2 item 4 for free rather than by eye. **This is the part that stops regression**, and it is worth more than any one fix.
- **Record the rules in `events-frontend/AGENTS.md`** (§12.4) — the frontend file, not the root one, since that is what an agent editing a `.vue` file loads.
- **Defer** the FullCalendar assessment and the public accessibility statement (§10 item 4) to the go-live checklist.

### 12.4 `events-frontend/AGENTS.md` — accessibility rules to add

The file currently documents the stack, the commands and the conventions, but says nothing about accessibility — so every good practice in §12.1 is folk
knowledge that survives only as long as someone imitates the surrounding code. Proposed section:

> ## Accessibility
>
> **Target: WCAG 2.1 Level AA.** These rules encode what the codebase already does — follow them rather than rediscovering them.
>
> - **Every interactive element needs an accessible name.** Icon-only controls carry an `aria-label`. Where a `title` tooltip is also present, derive both from
>   **one** `computed` so they cannot drift (see the theme toggle in `App.vue`).
> - **Decorative SVGs get `aria-hidden="true"`.** Meaningful images get a real `alt`; `alt=""` is correct only when the image adds nothing the text does not
>   already say.
> - **Every view renders exactly one `<main>`.** Detail views inherit it from `BaseDetailView` — do not add a second.
> - **Never remove a focus indicator.** `outline-none` is acceptable only when paired with a `focus-visible:` ring, as in `components/ui/button/index.ts`.
> - **Prefer a reka-ui / shadcn-vue primitive** over a hand-rolled interactive component. They handle focus management, keyboard interaction and ARIA that a
>   bespoke `div` will not.
> - **Form controls need a label** — a `<label>` element, or an `aria-label` when the design has no visible label (as in `EventFilterBar.vue`).
> - **Write e2e selectors by role and accessible name** (`getByRole('link', { name: … })`). This is the house style *and* it makes the suite an accessibility
>   regression test.
> - **Colour is never the only carrier of meaning** (1.4.1), and new colour pairs must clear 4.5:1 for body text, 3:1 for large text and UI boundaries (1.4.3,
>   1.4.11). `npm run test:e2e` includes an axe sweep — if it fails on contrast, fix the token, do not silence the rule.
> - **Do not disable a `vuejs-accessibility` lint rule** to make a build pass. Fix the markup, or raise it.

## 13. Decision log

Every question this plan opened, and how it was answered. All were settled on 2026-08-07.

| # | Question                | Decision                                                                                                              | Where   |
|---|-------------------------|-----------------------------------------------------------------------------------------------------------------------|---------|
| 1 | Imprint address         | Rent a *ladungsfähige Anschrift* from Postflex, ordered after the domain registration; guarded placeholder until then | §8.3    |
| 2 | Legal-page language     | English only now; German ships in the same release as the German UI, and becomes authoritative at that point          | §6.1    |
| 3 | First public version    | `0.1.0`; `main` carries `0.1.0-SNAPSHOT`; `-SNAPSHOT` renders unlinked                                                | §4.7    |
| 4 | `package.json` version  | Mirrors the Gradle version, kept in step by hand, without the `-SNAPSHOT` suffix                                      | §4.6    |
| 5 | Actuator                | Expose `/actuator/info` internally **and** serve `GET /meta` publicly — same bean, different consumers                | §4.4    |
| 6 | Code of Conduct         | Adopt Contributor Covenant **3.0** (not GitHub's built-in 2.1 template)                                               | Phase 6 |
| 7 | Donations / sponsorship | Possible later, not this iteration. `FUNDING.yml` first; on the site, link out — never embed                          | §8.4    |
| 8 | Localisation            | English + German, as the immediate follow-up; constrains how copy is written now                                      | §6.2    |
| 9 | Accessibility           | Target **WCAG 2.1 Level AA**; close the two Level A gaps and add automated checking in Phase 1                        | §12.3   |

**One item remains open, and it blocks nothing here:** the logging decisions in §7.5.1 (whether Traefik and the nginx frontend container log real client IPs,
truncation, retention, and where retention is enforced). They depend on infrastructure that does not exist yet, and Phases 1–6 can all proceed without them.

Two smaller things to settle in passing, both of which are the *same* answer used in three places: the **role email address** (imprint, privacy notice, and the
Code of Conduct reporting route), and the **`Last reviewed:` date** convention on the privacy page.

---

## 14. References

- [Spring Boot Actuator — info endpoint, build & git contributors](https://reflectoring.io/spring-boot-info-endpoint/) · [gradle-git-properties](https://github.com/n0mer/gradle-git-properties)
- [OSS Review Toolkit](https://github.com/oss-review-toolkit/ort) · [ORT introduction](https://oss-review-toolkit.org/ort/docs/intro)
- [GDPR full text](https://gdpr-info.eu/) —
  esp. [Art. 6](https://gdpr-info.eu/art-6-gdpr/), [Art. 13](https://gdpr-info.eu/art-13-gdpr/), [Art. 21](https://gdpr-info.eu/art-21-gdpr/), [Art. 28](https://gdpr-info.eu/art-28-gdpr/)
- [GDPR and log data](https://www.termsfeed.com/blog/gdpr-log-data/) · [IP addresses as personal data](https://www.cookieyes.com/blog/ip-address-personal-data-gdpr/)
- [Impressumspflicht nach § 5 DDG (eRecht24)](https://www.e-recht24.de/artikel/datenschutz/209.html) · [IHK: Informationspflichten im Internet](https://www.ihk.de/chemnitz/recht-und-steuern/rechtsinformationen/internetrecht/pflichtangaben-im-internet-die-impressumspflicht-4401580)
- Address privacy
  (§8): [ladungsfähige Anschrift ≠ Meldeadresse](https://zerodox.de/blog/ladungsfaehige-anschrift-meldeadresse) · [c/o-Adresse im Impressum](https://zerodox.de/blog/ratgeber-impressum-co-adresse) · [Impressum ohne Privatadresse](https://flexdienst.de/impressum-ohne-privatadresse-so-schuetzt-du-deine-adresse-rechtssicher/) · [provider price comparison](https://impressum-generator.de/geschaeftsadresse-mieten)
- [Pflichtangaben der Datenschutzerklärung (Art. 13)](https://giel-rechtsanwalt.de/allgemein/was-muss-in-eine-datenschutzerklaerung-hinein-webseite/)
- [FSF licence list and comments](https://www.gnu.org/licenses/license-list.html) · [Source-available licences (SSPL, BSL, Elastic v2)](https://www.softwareseni.com/understanding-open-source-licenses-from-permissive-bsd-to-restrictive-business-source-license-and-sspl/)
- [WCAG 2.1](https://www.w3.org/TR/WCAG21/) — §12; see also [How to Meet WCAG (quick reference)](https://www.w3.org/WAI/WCAG21/quickref/), which is the
  practical form of it
- [GitHub: setting contributor guidelines](https://docs.github.com/en/communities/setting-up-your-project-for-healthy-contributions/setting-guidelines-for-repository-contributors) · [GitHub: configuring issue templates](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/configuring-issue-templates-for-your-repository) · [GitHub: adding a code of conduct](https://docs.github.com/en/communities/setting-up-your-project-for-healthy-contributions/adding-a-code-of-conduct-to-your-project)
- [Contributor Covenant 3.0](https://ethicalsource.dev/projects/contributor-covenant-3/) — the current version; GitHub's built-in template is still 2.1
- [GitHub: displaying a sponsor button (
  `FUNDING.yml`)](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/displaying-a-sponsor-button-in-your-repository) —
  §8.4
