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

- [ ] Add Authentication & Authorization (best practice for Spring? Keycloak, at least locally for testing?)
- [ ] Caching (BFF)
- [ ] Protect the public BFF API (rate limiting, DDoS; API gateway?)
- [ ] Create a test data set — reusable as test fixtures **and** to populate the local DB

---

## UI / UX / Branding

- [ ] Full frontend UX pass — what's missing / improvable? (cross-check the vision + branding docs)
- [ ] Verify responsive design + look on mobile
- [x] "Sold Out" label + filter to exclude sold-out events (PR #57)
- [x] Free-events filter + "Free" badge (import-time detection via `detectFree`)
- [ ] Audit that all **user-facing** surfaces read "Event Junkie" (internal stays "Event Checker")
- [ ] Decide on a display/hero typeface vs. staying all-Geist (BRANDING §5.3)
- [ ] "Venue or event missing? Let us know" form (→ GitHub issues?)
- [ ] Feedback form (or link to GitHub issues)
- Design tooling to try: Stitch, "trainspotting"-inspired design — what's the best tool for generating a website design?

## Frontend & BFF

- [ ] Venues page (with map) — consumes the existing `GET /venues` list endpoint
- [ ] District filter/feed → add a `district` column to the venue table
- [ ] Sitemap (still worthwhile for SEO?)
- [ ] RSS feed for newly imported events
- [ ] I18N / L10N + translations
- Note: `GET /artists`, `GET /venues`, `GET /promoters` list endpoints exist and are smoke-tested,
  but only their `/{slug}` detail counterparts have UI pages yet.

## Importer / Data

- [x] Detect free events at import (`detectFree`: €0 price or free-entry phrases → `free` flag)
- [ ] Enrich venues: type (club/bar/concert hall), description, image/photo, genres, event types
- [ ] Improve importer Swagger UI (match the BFF)
- [ ] Implement more importers/scrapers (see EVENT_DATA_SOURCES.md)
    - [ ] Strategy to implement the remaining importers fast — but still clean, robust, fully tested
    - [ ] Create a prompt/skill for scaffolding a new importer
    - [ ] Standardize/simplify existing importers + scrapers where it helps
- [ ] Housekeeping: policy for when to delete old events from the DB

## Operations & Hardening

- [ ] Logging: always attach context (event id, artist id, …)
- [ ] Checkov scan (if it makes sense)
- [ ] Infra/tooling update checker beyond Dependabot (Renovate?)
- [ ] Review useful security workflows → https://github.com/enorm-labs/event-checker/actions/new?category=security
- [ ] Dashboard for analysing the data (Superset, Kibana, Grafana, …?)
- [ ] Enable agentic workflows (continuous refactoring/docs) → https://github.github.com/gh-aw/

## Legal / Compliance (before going live)

- [ ] Imprint (Impressum)
- [ ] DSGVO / GDPR
- [ ] Accessibility review
- [ ] Display used FOSS / attributions
- [ ] Link to the GitHub repository
- [ ] Confirm legality of scraping events and displaying them

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
- [ ] Generate a Mermaid domain class diagram via Gradle
- [ ] Community/repo health files: CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, SUPPORT
  (example: [gitfolio](https://github.com/github-samples/gitfolio))
- [ ] Repository best-practices pass (follow GitHub docs)
- [ ] Create a public Roadmap
- [ ] Create a template repository (Enterprise + private):
    - [ ] `.github/` with workflows, instructions, skills, prompts, agents
    - [ ] README, CONTRIBUTING, LICENSE, etc.
    - [ ] Check for good existing templates first; see also the OTR service template; add scaffolding

---

## 🔵 Someday / Vision

Bigger bets and post-MVP features. Details in [docs/VISION_ROADMAP_IDEAS.md](docs/VISION_ROADMAP_IDEAS.md).

- iCal support; export/import calendar to Google Calendar or file
- Login / profile: favourites (Merkliste), reminders, favourite venues/artists, notifications,
  customizable start page, RSVP ("interested"/"going"), recommendations
- User/venue-submitted events with review-before-publish (→ RBAC / Keycloak)
- Social layer: connect with friends, see friends' interest/attendance
- Venue & artist profiles (with links); venues browsable by genre + location
- Rank events by popularity (RSVPs) and by artist popularity
- Integrate Spotify / Deezer / SoundCloud / Resident Advisor (notify when favourite artists play)
- Club map — events nearby
- Public API for third-party apps → API management (subscriptions, keys)
- Expand beyond Berlin to other cities
