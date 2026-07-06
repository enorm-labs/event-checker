# Vision, Roadmap & Ideas

The longer-form product context: **what** Event Junkie is and **where** it's headed.
The granular, actionable backlog lives in **[TODO.md](../TODO.md)** (the single source of truth for
planned work); brand, voice and design live in **[BRANDING.md](BRANDING.md)**. This document stays at
the level of _direction_ — it links down to TODO.md rather than duplicating tasks.

---

## Vision

### What it is

Event Junkie is a discovery app for **music events in Berlin** — concerts, club nights, festivals —
aggregated from venue and promoter websites into one fast, filterable place, always linking back to
the original source.

- Public app name: **Event Junkie** (→ event-junkie.de). **Event Checker** stays the internal/repo name.
- Tagline: _"Can't get enough of Berlin."_ (voice & branding: see [BRANDING.md](BRANDING.md))

### Who it's for & positioning

Think Resident Advisor, Bandsintown, Eventbrite or Songkick — **but for all of Berlin's music scene,
and better**:

- **All genres, not only Techno/Electronic** — the incumbents skew electronic or ticketed-only.
- **Small and underground events too**, not just the big ticketed shows.
- Closest existing references: [clubguideberlin.de](https://www.clubguideberlin.de/),
  [gaesteliste030.de](https://www.gaesteliste030.de/).

### Scope

- **In scope now:** music events across Berlin venues (clubs, concert halls, bars, …).
- **Maybe later:** other cities; potentially non-music events — deliberately **out of scope** for the MVP.

### Guiding principles

- **Aggregate and link back, don't republish** — store only the structured fields needed and link to the
  source for full details (respect venue copyright; see [ADR-007](adr/ADR-007_WEB_SCRAPING_STRATEGY.md)).
- **Data quality over volume** — better to show fewer, correct events than a noisy firehose.
- **Fast, clean, mobile-first discovery.**
- **Be a good scraping citizen** — polite rate limits, transparent User-Agent, off-peak scheduling.

---

## Roadmap

High-level phases and the shape of the journey — **not** a checklist. The concrete tasks behind each
phase live in [TODO.md](../TODO.md) under the referenced sections.

### Phase 0 — Foundation ✅ _(built)_

The core product exists end-to-end:

- **Backend** — Kotlin + Spring Boot on a reactive stack (WebFlux, R2DBC, Flyway), split into a
  Spring-Modulith **importer** and a public **BFF** (see the [ADRs](adr/)).
- **Importers** — six Berlin venues live: Cassiopeia, Privatclub, Madame Claude, Astra, Lido, SO36,
  with a `/scaffold-importer` skill for adding more.
- **Frontend** — Vue 3 app with a calendar view, event/artist/venue/promoter detail pages, and
  district / free / sold-out filters plus genre tags.

### Phase 1 — MVP / Go-live 🔴 _(in progress)_

Turn the working prototype into a live public product.
→ TODO: **🔴 Now**, **🟠 Next**, **Legal / Compliance**.

- Cloud platform, domain (**event-junkie.de**), CI/CD, and a first deploy.
- Hardening the path to production: auth & authorization, BFF caching, API protection, a reusable
  test/seed dataset, and the go-live checklist (security, monitoring, backups, recovery).
- Legal readiness: Impressum, GDPR, accessibility, FOSS attributions, scraping legality.

### Phase 2 — Coverage & polish 📈 _(post-launch)_

Make it comprehensive, discoverable and pleasant.
→ TODO: **Importer / Data**, **Frontend & BFF**, **UI / UX / Branding**.

- Scale importer coverage toward the full venue list in
  [EVENT_DATA_SOURCES.md](EVENT_DATA_SOURCES.md); enrich venue metadata.
- An **admin imports-status dashboard** to watch import health and failures.
- Venues page with map, a full UX/mobile pass, and SEO surfaces (sitemap, RSS); i18n/l10n.
- "Missing event / venue" and feedback forms.

### Phase 3 — Accounts & personalization 👤 _(Expansion stage 1)_

Give people a reason to come back.
→ TODO: **🔵 Someday / Vision** (stage 1).

- Login / profile with **follow/favourite** for artists, venues, districts, promoters and genres —
  used both to filter events and to drive **notifications** (two steps, YouTube-style: 1. follow,
  2. get notified).
- Favourites (Merkliste), reminders, RSVP ("interested" / "going"), a customizable start page,
  recommendations.
- User/venue-submitted events with review-before-publish — needs RBAC (Keycloak).

### Phase 4 — Social & ecosystem 🤝 _(Expansion stage 2)_

Turn discovery into a network and open the data up.
→ TODO: **🔵 Someday / Vision** (stage 2).

- Social layer: connect with friends and see which events they're interested in or going to.
- Ranking by popularity (RSVPs) and by artist popularity; richer venue & artist profiles.
- Integrations with Spotify / Deezer / SoundCloud / Resident Advisor (notify when favourite artists play).
- Club map (events nearby), iCal export & calendar sync, and a public API with API management.

### Phase 5 — Beyond Berlin 🌍 _(bigger bets)_

Expand to other cities and broaden scope once the Berlin experience is strong.

---

## Idea sources & references

- Extended idea backlog:
  [Google Doc](https://docs.google.com/document/d/1UkxdJECxvB6noW-n8dzX-r0du18M9-ggp6iWeapHpvI/edit).
- Competitors / inspiration: Resident Advisor, Bandsintown, Eventbrite, Songkick;
  clubguideberlin.de, gaesteliste030.de.
- Alternative names considered (not chosen): Berlin Club Guide (berlinclubguide.de / berlin-clubs.de).
