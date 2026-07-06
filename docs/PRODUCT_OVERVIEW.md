# Event Junkie — Product Overview

> _"Can't get enough of Berlin."_

A factual, present-tense snapshot of **what Event Junkie is, what it does, and where it stands today**.
It's meant to be a shared reference — for onboarding, for the README, and as raw material for the
**branding** work (see [BRANDING.md](BRANDING.md)). For where the product is _headed_, see
[VISION_ROADMAP_IDEAS.md](VISION_ROADMAP_IDEAS.md); for the task backlog, see [TODO.md](../TODO.md).

Public app name: **Event Junkie** (→ event-junkie.de). **Event Checker** is the internal/repo name.

---

## What is Event Junkie?

Event Junkie is a **music-event discovery app for Berlin**. It automatically collects concerts, club
nights and festivals from venue and promoter websites and brings them together into one fast,
filterable, mobile-friendly place — always linking back to the original source for tickets and details.

Think Resident Advisor, Bandsintown or Songkick — but for **all** of Berlin's music scene, not just the
big electronic/ticketed shows.

## The problem it solves

- **Berlin's live-music scene is huge but scattered.** What's on is spread across dozens of venue and
  promoter sites, each with its own layout, quirks and gaps. There's no single, reliable place to just
  _browse what's happening_.
- **Existing aggregators are narrow.** They skew toward Techno/electronic or only list the big, ticketed
  events — small, underground and non-electronic shows fall through the cracks.
- **Discovery is hard.** It's tedious to answer simple questions like _"what's on near me this weekend,
  in my genre, that's free or cheap?"_ when the data lives on 40+ separate websites.

Event Junkie aggregates those sources into **one clean, searchable feed**, so finding something to go to
takes seconds instead of a dozen browser tabs.

## What it lets you do

- **Browse and search** upcoming events, with a **calendar view** and a **today** view.
- **Filter** by what you actually care about: date range, event type, **Berlin district** (all 12
  boroughs), **genre**, **price range**, **free-only**, and **exclude sold-out** — plus free-text search
  over titles.
- **Drill into details:** dedicated pages for each **event, venue, artist and promoter**, cross-linked so
  you can jump from an artist to all their Berlin dates, or from a venue to its full programme.
- **See the signal at a glance:** "Free" and "Sold Out" badges, event status (e.g. cancelled/postponed),
  door/start times, prices, line-ups and genre tags.

---

## Current Status

**🚧 In active development — not yet publicly deployed.** The core product works end-to-end locally; the
path to a public launch (hosting, domain, auth, legal) is tracked in [TODO.md](../TODO.md).

### Live features

**Discovery frontend** (Vue 3)
- Home, **calendar**, and event **search/list** pages, plus **event / venue / artist / promoter** detail
  pages and an About page.
- Filtering by event type, district (12 boroughs), genre, price range, free-only and exclude-sold-out,
  with free-text search; "Free" and "Sold Out" badges.

**Public read API — BFF** (`/api/…`, OpenAPI/Swagger documented)
- Event search with the full filter set above **+ venue / artist / promoter** filters, pagination and
  sorting; plus **today** and **date-range calendar** endpoints and per-slug detail.
- List + detail endpoints for **venues, artists, promoters and genres**.

**Automated data aggregation — Importer**
- **Six Berlin venues live:** Cassiopeia, Privatclub, Madame Claude, Astra Kulturhaus, Lido, and SO36.
- **Scheduled imports** with **change detection** (ETag / Last-Modified), **per-host politeness
  throttling**, deduplication, and **stale-event cleanup**.
- **Per-source status tracking + retry**, managed via an admin API (create/enable/trigger/retry sources).
- **Data enrichment at import:** free-event detection, sold-out and event-status detection, and automatic
  creation + linking of **artists (with roles/billing order), promoters and genre tags**.
- A **`/scaffold-importer`** skill that turns adding a new venue into a guided, tested workflow.

**Data model**
- Events, venues, artists, promoters, genre tags and import sources, with many-to-many links
  (event↔artist, event↔promoter, event↔genre).

### Engineering foundation

- Reactive **Kotlin + Spring Boot 4** backend (WebFlux, R2DBC, Flyway) split into a **Spring-Modulith
  importer** and a public **BFF**, backed by **PostgreSQL**; **Vue 3** frontend.
- Decisions captured as **ADRs**; unit/integration tests across importer, BFF and frontend.

### Not there yet

User accounts & personalization, notifications, a venues map, broader venue coverage (beyond the current
six), and a public deployment. These are the roadmap — see
[VISION_ROADMAP_IDEAS.md](VISION_ROADMAP_IDEAS.md) and [TODO.md](../TODO.md).

---

## Using this for branding

This overview is deliberately concrete about the **audience** (Berlin music-goers who want _everything_,
not just the big electronic nights), the **core value** (one fast, filterable feed for a fragmented
scene), and the **personality hooks** (comprehensive, local, for the dedicated fan — _"can't get enough
of Berlin"_). Those are the angles to sharpen into voice, logo, colour and imagery in
[BRANDING.md](BRANDING.md).
