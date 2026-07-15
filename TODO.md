# TODO — Backlog

Single source of truth for planned work. Longer-form context lives in
[docs/VISION_ROADMAP_IDEAS.md](docs/VISION_ROADMAP_IDEAS.md) and
[docs/BRANDING.md](docs/BRANDING.md); this file is the actionable backlog.

Rough priority: **Now** → **Next** → grouped backlog → **Someday / Vision**.
Public name is **Event Junkie**; internal/repo name stays **Event Checker**
(see the BRANDING naming rule — don't "fix" internal identifiers).

---

## 🔴 Now (path to go-live)

- [ ] Choose a cloud platform / runtime environment (AWS, GCP, …)
- [ ] Register the domain **event-junkie.de**
- [ ] Create release + deploy workflows (CI/CD)
- [ ] Deploy to the chosen cloud platform
- [ ] Fix Dependabot security issues → https://github.com/enorm-labs/event-checker/security/dependabot
- [ ] Go-live checklist: legal, security, SEO, monitoring, alerting, dashboards, backups, recovery

## 🟠 Next

- [ ] Add Authentication & Authorization (best practice for Spring? Keycloak, at least locally for testing? Support Passkey?)
- [ ] Caching (BFF)
- [ ] Protect the public BFF API (rate limiting, DDoS; API gateway?)
- [ ] Create a test data set — reusable as test fixtures **and** to populate the local DB

---

## UI / UX / Branding

- [ ] Full frontend UX pass — what's missing / improvable? (cross-check the vision + branding docs)
- [ ] Improve **branding & UI/visual design** — a dedicated visual pass beyond the UX audit
  (colour, type, spacing, components, logo, iconography, imagery, motion), aligned with [BRANDING.md](docs/BRANDING.md)
- [ ] Verify responsive design + look on mobile
- [ ] Audit that all **user-facing** surfaces read "Event Junkie" (internal stays "Event Checker")
- [ ] Decide on a display/hero typeface vs. staying all-Geist (BRANDING §5.3)
- [ ] "Venue or event missing? Let us know" form (→ GitHub issues?)
- [ ] Feedback form (or link to GitHub issues)
- [ ] Evaluate design tools & AI models for UI + branding — which best generate/refine a site design?
  Candidates: Google Stitch, v0, Lovable, Figma (+ AI / Make), plus image models (e.g. Midjourney) for
  visuals/mood boards. Explore the "trainspotting"-inspired direction.

## Frontend & BFF

- [ ] Add map to venues overview page
- [ ] Add venue description and maybe other metadata to venue detail page
- [ ] Filter venues by district
- [ ] Filter events and venues by venue type and genre
- [ ] Filter events by a specific venue (e.g. from the venue detail page)
- [ ] Browse/see past events — an archive view (decide retention + UX; ties into the housekeeping delete policy under Importer / Data)
- [ ] Reduce or group the displayed genres — too many distinct tags; needs a grouping/taxonomy decision (UX + data)
- [ ] Decide whether to display event **descriptions** and **source images** — copyright/licensing plus traffic to small sites; if images: store/cache/proxy vs.
  hotlink vs. omit (see the Legal/Compliance copyright item)
- [ ] Sitemap (still worthwhile for SEO?)
- [ ] RSS feed for newly imported events
- [ ] I18N / L10N + translations
- Note: `GET /artists`, `GET /venues`, `GET /promoters` list endpoints exist and are smoke-tested,
  but only their `/{slug}` detail counterparts have UI pages yet.

## Importer / Data

- Known per-importer gaps & missing-data limitations are catalogued in
  [docs/IMPORTER_KNOWN_ISSUES.md](docs/IMPORTER_KNOWN_ISSUES.md) — pull from there when picking up work.

**Data quality — normalize, validate, enrich:**

Strategy & sequencing: [docs/DATA_QUALITY_STRATEGY.md](docs/DATA_QUALITY_STRATEGY.md)
(Measure → Prevent → Fix → Systematize).

- [ ] **(Pillar 1 — Measure)** Data-quality report: `GET /api/admin/data-quality` +
  scheduled summary log + Micrometer gauges — per-source counts of artist-less concerts,
  `OTHER`-typed events, and missing genre/promoter/price/start-time. Plus a `/worklist`
  endpoint (offending events per metric) so stewards fix via the existing Event API — no
  bespoke frontend yet. Persist daily metric snapshots (`data_quality_snapshot`) so trends
  are chartable in an external BI tool (see the *Dashboard* item under Operations).
  Plan: [docs/DATA_QUALITY_PILLAR_1_PLAN.md](docs/DATA_QUALITY_PILLAR_1_PLAN.md).
- [ ] **(Pillar 2 — Prevent)** Golden fixture tests from real scraped HTML for all four
  normalizers, plus a boundary validation gate that flags obviously-bad output
  (empty artist after stripping, artist == non-artist pattern, genre == event title)
  into the curation queue instead of persisting it silently.
- [x] **(Pillar 3 — Fix)** Title-as-headliner extraction for venues without a `Support:` signal
  (Privatclub, Cassiopeia, Badehaus) — recovers the ~40% of concerts previously stored with no
  artist. Done via `buildArtistsForEventType` / `headlinersFromTitle`; Cassiopeia's ambiguous
  titles are guarded by a widened `isNonArtistName` festival filter. **Still TODO: a one-off
  backfill re-scrape** — existing rows keep no artist until re-imported.
- [ ] **(Pillar 4 — Systematize)** AI-assisted data quality in the importer (one capability,
  several uses): detect/extract artist names from titles, validate event types, enrich missing
  fields (genres, event types), and fix bad values (artist names, promoter names, …) —
  cross-checking the event source page and the wider web where useful. Runs
  *after* the deterministic normalizers, human-in-the-loop via the admin review UI.
  **Needs ADR-012 (AI-Assisted Data Quality)** — new external dependency, cost/latency,
  non-deterministic output.
- [ ] **(Decision — ADR candidate)** Curated-vocabulary storage: code vs. data. Move the
  denylists / synonym maps / corrections (`NON_ARTIST_NAMES`, `NAME_CORRECTIONS`, genre
  synonyms, `ACRONYMS`) from hardcoded Kotlin to steward-editable DB tables so fixes land
  without a redeploy — vs. keeping them as tested code fixed via PR. Spike + ADR before
  Pillar 4's human-in-the-loop needs live editing; blocks nothing in Pillars 1–3.
  (Strategy §6.)
- [ ] Enrich venues: type (club/bar/concert hall), description, image/photo, genres, event types
- [ ] Enrich promoters: description, image, and corrected display names
- [ ] Check & fix venue districts, addresses, and geo-coordinates

**Importer coverage & parsing:**

- [ ] Scrape events in multiple languages (English + German) where the source offers it (e.g. Berghain) —
  first audit which event sources are actually multi-language
- [ ] Update importers to scrape/parse **all** available events via the site's navigation/pagination
  (not just the first page)
- [ ] Review events typed `OTHER` — should we add new values to the event-type enum?
- [ ] Add events manually for venues that have no website — plus a plan for keeping those up to date

**Admin tooling & maintenance:**

- [ ] Admin frontend to review, enrich & fix event data in one place — sort/filter events by
  missing fields; edit artist/promoter names, event types, genres, …
- [ ] Admin imports-status dashboard — surface import states and especially **failed** imports.
  Start with Importer API endpoints + an admin IntelliJ HTTP Client collection; a proper admin
  frontend later. (`EventSourceController` already exposes per-source status + retry — build on it.)
- [ ] Improve importer Swagger UI (match the BFF)
- [ ] Housekeeping: policy for when to delete old events from the DB

**More importers:**

- [ ] Implement more importers/scrapers (see EVENT_DATA_SOURCES.md)
    - [ ] Strategy to implement the remaining importers fast — but still clean, robust, fully tested
    - [ ] Standardize/simplify existing importers + scrapers where it helps
    - [ ] Find venues we may have missed — cross-check [theclubmap.com](https://www.theclubmap.com/music-style/),
      Resident Advisor, and the web
    - [ ] Evaluate bars that host DJs / live music / other events as sources
    - [ ] Check promoters already in the DB and scan their sites for events
    - [ ] Cover events at special/one-off locations (e.g. Durchlüften Festival @ Humboldtforum,
      Tempelhofer Feld, Olympiastadion)
    - [ ] Radio-station event listings (RadioEins, FluxFM, StarFM, …)
    - [ ] Consider importing from Resident Advisor — confirm legality first (probably not allowed)

## Operations & Hardening

- [ ] Logging: always attach context (event id, artist id, …)
- [ ] Checkov scan (if it makes sense)
- [ ] Infra/tooling update checker beyond Dependabot (Renovate?)
- [ ] Review useful security workflows → https://github.com/enorm-labs/event-checker/actions/new?category=security
- [ ] Dashboard for analysing the data (Superset, Kibana, Grafana, …?) — also the intended
  surface for the **data-quality metrics/trends** (Pillar 1 exposes them via a
  `data_quality_snapshot` table for SQL-based BI and Micrometer/Prometheus for Grafana;
  see [docs/DATA_QUALITY_STRATEGY.md](docs/DATA_QUALITY_STRATEGY.md) §4)
- [ ] Enable agentic workflows (continuous refactoring/docs) → https://github.github.com/gh-aw/

## Legal / Compliance (before going live)

- [ ] Imprint (Impressum)
- [ ] DSGVO / GDPR
- [ ] Accessibility review
- [ ] Display used FOSS / attributions
- [ ] Link to the GitHub repository
- [ ] Confirm legality of scraping events and displaying them
- [ ] Clarify copyright/licensing of event **descriptions** and **images** per source — are we allowed to
  store/display them? Track a copyright/license status per event source (drives the description/image
  display decision under Frontend & BFF)

## Tooling, AI Agents & Skills

- [ ] Multiple/path-specific instruction files (at least backend + frontend) →
  [docs](https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/add-custom-instructions/add-repository-instructions#creating-path-specific-custom-instructions)
- [ ] Create more prompts/skills/agents:
    - [ ] Feature planning + spec creation (interview → spec → plan; see [spec-kit](https://github.github.com/spec-kit/))
    - [ ] Code review agent
    - [ ] Documentation-update agent
    - [ ] Security agent
    - [ ] UI/UX agent
    - [ ] Refactoring / code-quality agent (behavior-preserving)
    - [ ] Architecture-review agent
    - [ ] ADR-authoring prompt
- [ ] Evaluate/steal ideas (don't necessarily install): [awesome-copilot](https://github.com/github/awesome-copilot),
  [superpowers](https://github.com/obra/superpowers), [get-shit-done](https://github.com/gsd-build/get-shit-done)
  — recommendation on record: keep AGENTS.md as the source of truth; add optional prompt files, don't adopt always-on ceremony.
- [ ] Try [Repomix](https://repomix.com/) (+ [GH Actions](https://repomix.com/guide/github-actions))
- [ ] Consider a `BACKLOG.md` context-engineering approach
  ([reference](https://www.codecentric.de/wissens-hub/blog/strukturierte-migration-mit-claude-code-context-engineering-statt-prompt-engineering))
- Note: IntelliJ Copilot Chat now supports the "Copilot CLI" provider, so global `~/.copilot/skills/` are usable there too.

## Docs, Repo & Templates

- [ ] **To consider:** rename the repo (and internal `event-checker` references) to `event-junkie` —
  would collapse the public/internal split. Note: this **reverses the current BRANDING naming rule**
  (§ "Naming rule"); if pursued, update BRANDING.md accordingly. Scope: repo name, Gradle modules,
  packages, DB schema, ADRs, docs.
- [ ] Clean up KDoc comments across the codebase — drop boilerplate/irrelevant comments, keep the rest meaningful
- [ ] Generate a Mermaid domain class diagram via Gradle
- [ ] Community/repo health files: CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, SUPPORT
  (example: [gitfolio](https://github.com/github-samples/gitfolio))
- [ ] Repository best-practices pass (follow GitHub docs)
- [ ] Create a public Roadmap (seed it from the phased roadmap in [docs/VISION_ROADMAP_IDEAS.md](docs/VISION_ROADMAP_IDEAS.md))
- [ ] Create a template repository (Enterprise + private):
    - [ ] `.github/` with workflows, instructions, skills, prompts, agents
    - [ ] README, CONTRIBUTING, LICENSE, etc.
    - [ ] Check for good existing templates first; see also the OTR service template; add scaffolding

---

## 🔵 Someday / Vision

Bigger bets and post-MVP features. Details in [docs/VISION_ROADMAP_IDEAS.md](docs/VISION_ROADMAP_IDEAS.md).

- iCal support; export/import calendar to Google Calendar or file
- Chatbot and/or MCP server to find events and answer questions about events, artists, venues, districts, promoters, genres, etc.
- **Expansion stage 1 — Login / profile:** follow/favourite artists, venues, districts, promoters,
  genres (…) to filter events and drive notifications — two steps, YouTube-style: (1) follow,
  (2) get notified. Plus favourites (Merkliste), reminders, customizable start page,
  RSVP ("interested"/"going"), recommendations. (→ RBAC / Keycloak)
- **Expansion stage 2 — Social layer:** connect with friends to see which events they're interested
  in or going to (interest/attendance).
- User/venue-submitted events with review-before-publish (→ RBAC / Keycloak)
- Venue & artist profiles (with links); venues browsable by genre + location
- Rank events by popularity (RSVPs) and by artist popularity
- Integrate Spotify / Deezer / SoundCloud / Resident Advisor (notify when favourite artists play)
- Club map — events nearby
- Public API for third-party apps → API management (subscriptions, keys)
- Expand beyond Berlin to other cities
