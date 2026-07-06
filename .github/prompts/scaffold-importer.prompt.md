# Scaffold a New Importer

Add a new venue/promoter event importer (scraper) to `events-importer`, end to end: enum value,
parser + importer classes, HTML snapshot fixtures, tests, and dev-seed wiring — following the
patterns in ADR-007 and the existing importers.

**Arguments**: `$ARGUMENTS` should name the venue and its listing URL (e.g. `SO36 https://so36.com/programm/`).
If either is missing, ask for it before starting.

## Important

- Run git commands with the pager disabled (`git --no-pager ...`).
- Read [ADR-007 Web Scraping Strategy](../../docs/adr/ADR-007_WEB_SCRAPING_STRATEGY.md) in full before writing
  any code — it is the source of truth for architecture, selector strategy, and scraping ethics. Also skim
  [EVENT_DATA_SOURCES.md](../../docs/EVENT_DATA_SOURCES.md) for the pre-analysed field mapping of the target venue.
- Use an existing importer as a template. Pick the closest match to the target's page structure:
    - **Single listing page** (all data on one page): `scraper/privatclub/` — implements `EventImporter` directly.
    - **List + detail pages** (summaries on a listing, full data on per-event pages): `scraper/cassiopeia/`,
      `scraper/madameclaude/`, `scraper/astra/`, `scraper/lido/` — extend `AbstractTwoPageWebsiteImporter`.
- Do **not** reinvent boilerplate. Reuse the shared extension helpers in the `scraper/` package
  (see step 4). New code should look like the code around it.

---

## 1. Reconnaissance — understand the target site first

Before writing anything, learn how the site is built and whether you're allowed to scrape it.

1. **Check `robots.txt`** (`<host>/robots.txt`). Honour any `Disallow` on the listing/detail paths.
   If scraping is disallowed, stop and report back rather than proceeding.
2. **Fetch the real listing HTML** and save it — you'll need it as a test fixture anyway:
   ```bash
   curl -sSL -A 'EventChecker/1.0 (+https://github.com/...)' '<listing-url>' \
     -o events-importer/src/test/resources/scraper/<venue>/<venue>-overview.html
   ```
   For list+detail sites, also fetch one or two representative detail pages
   (`<venue>-detail-<case>.html`), including edge cases you want regression coverage for
   (cancelled event, sold-out, free entry, missing date).
3. **Classify the page technology** and pick a strategy (ADR-007 §Decision):
    - **Server-rendered HTML** (~80%): Jsoup parsing works directly. This is the happy path.
    - **Structured data present** (`<script type="application/ld+json">` `schema.org/MusicEvent`,
      Microdata): prefer it — it's the most stable source. See `PrivatclubOverviewPageScraper`'s
      JSON-LD handling and `AstraWebsiteImporter` for examples.
    - **JS-rendered SPA / cookie wall**: Playwright is **not** in the project yet (ADR-007 §3). If the
      content isn't in the raw HTML, stop and flag it — this needs the Playwright dependency added first,
      which is a separate decision.
4. **Decide the page pattern**: single-page vs. list+detail vs. paginated. ADR-007 §"Single Entry URL"
   and §"Pagination — First Page Only" govern this. Import the **first page only**; if the venue truly
   needs multi-page crawling, loop inside `importEvents()` (do not change the interface).

## 2. Add the `EventSource` enum value

In `scraper/EventSource.kt`, add a value with a KDoc comment describing the venue and its tech, matching
the existing entries. The name becomes the `source_type` key and the `sourceId` prefix (lowercased):

```kotlin
/** SO36 Berlin – hand-coded HTML program listing at /programm. */
SO36,
```

## 3. Create the venue sub-package

New importers live in their own sub-package: `scraper/<venue>/`. Create:

- **`<Venue>OverviewPageScraper.kt`** — a pure parser (no I/O) that takes a Jsoup `Document` + base URL
  and returns `List<ScrapedEvent>`. This is where CSS selectors live.
- **`<Venue>DetailPageScraper.kt`** — *(list+detail sites only)* pure parser returning `ScrapedEvent?`
  for a single detail page.
- **`<Venue>WebsiteImporter.kt`** — the `@Component` that owns HTTP fetching and wires the scrapers.
    - **Single-page**: implement `EventImporter` directly (template: `PrivatclubWebsiteImporter`). Fetch via
      `HtmlFetcher.fetch(url, etag, lastModified)`, handle `FetchResult.NotModified` / `FetchResult.Success`,
      return `ImportResult.NotModified` / `ImportResult.Success(events, etag, lastModified)`.
    - **List+detail**: extend `AbstractTwoPageWebsiteImporter` (template: `CassiopeiaWebsiteImporter`) and
      implement `scrapeOverview`, `scrapeDetail`, and `fillGapsFromOverview` (fill only fields the detail
      page can't supply, e.g. image URL from the overview). The base class owns fetch orchestration,
      per-detail-page error fallback, and dropping events with unresolved dates.

Set `override val eventSource = EventSource.<VENUE>`.

## 4. Reuse the shared scraper utilities

Do not hand-roll extraction/parsing that already exists in the `scraper/` package (ADR-007 §Shared Scraping
Utilities). Use these:

- **`ScrapingExtensions.kt`** — `Element.textAt(css)`, `attrAt(css, attr)`, `imgSrcAt(css)`, `hrefAt(css)`,
  `hasVisibleWebflowFlag(...)`, and `resolveUrl(baseUrl, href)`. These already null-out blanks and reject
  non-absolute URLs.
- **`DateParsingExtensions.kt`** — `parseTime(text)`, `parseIsoDate(str)`, `parseIsoTime(str)`, `HH_MM_FORMATTER`.
  Add a venue-specific formatter (e.g. German month names) only if the site's date format isn't covered.
- **`EventMappingExtensions.kt`** — `mapEventType(...)` / `mapGermanCategory(...)`, `isPlaceholderName(...)`
  (drop "TBA"/"N.N."), `buildArtistList(title, supportNames)`, `extractSupportFromSubtitle(...)`.

If you find yourself writing a genuinely reusable helper (used by 2+ venues), add it to the appropriate
extension file rather than the venue package, and note it in ADR-007's utility tables.

## 5. Selector strategy — build for durability

Selectors are the most fragile part of the pipeline. Follow ADR-007 §"Selector Strategy" preference order:
structured data (JSON-LD) > semantic HTML5 (`article`, `time[datetime]`, `h1`–`h6`) > ARIA roles >
`data-*` attributes > meaningful class names (`.event-title`) > **avoid** positional/presentational
(`div:nth-child(3)`, `.col-md-4`). Scope selectors to the narrowest semantic container and use `:has()`
for contextual matching.

Populate `ScrapedEvent` fields (see `scraper/ScrapedEvent.kt` for the full contract):

- **Required**: `title`, `eventDate`, `sourceUrl`, `sourceId`. Build `sourceId` as
  `"${EventSource.<VENUE>.sourceIdPrefix}$slug"` — a stable per-event identifier used for idempotent upserts.
  Derive the slug from the event's canonical URL/path, not from mutable text.
- **Optional**: subtitle, description, eventType (map to a known type; unclassifiable → let it default to
  `OTHER`), doors/start times, imageUrl, ticketUrl, genre, prices (`pricePresale`/`priceBoxOffice`/`priceNote`),
  `soldOut`, `free`, `status` (`SCHEDULED`/`CANCELLED`/`POSTPONED`/`RELOCATED`), `artists`, `promoters`.
- **Validate before returning** (ADR-007 best-practice #5): skip events with a blank title or unparseable
  date, logging a warning — never persist garbage. Wrap per-event parsing in a try/catch so one malformed
  event doesn't abort the whole import (see `PrivatclubOverviewPageScraper.scrape`).

## 6. Tests — snapshot-based regression guards

Every importer needs tests parsing the saved HTML fixtures (ADR-007 best-practice #4). Mirror the existing
test layout under `src/test/kotlin/de/norm/events/scraper/<venue>/`:

- **`<Venue>OverviewPageScraperTest.kt`** (and `<Venue>DetailPageScraperTest.kt` for list+detail) — parse
  the fixture with `Jsoup.parse(html, baseUrl)` and assert extracted fields: event count, a fully-populated
  representative event (all fields), and edge cases (cancelled/sold-out/free/missing-date).
- **`<Venue>WebsiteImporterTest.kt`** — mock `HtmlFetcher` with MockK (`coEvery { htmlFetcher.fetch(...) }`),
  assert `importEvents` returns `ImportResult.Success` with the right events, propagates ETag/Last-Modified,
  returns `NotModified` on `FetchResult.NotModified`, handles an empty page, and that `eventSource` matches
  the enum. Template: `PrivatclubWebsiteImporterTest`. For list+detail, stub both overview and detail fetches.

Conventions: JUnit 5 + Kotest matchers (`shouldBe`, `shouldHaveSize`, `shouldBeInstanceOf`) + MockK. Use a
fixed `Clock` if the scraper does year-rollover date inference, so tests are deterministic. Write `runTest {}`
test bodies as block statements (`= runTest { ... }` is fine here since they return `TestResult`), but for
plain `runBlocking` helpers remember the `: Unit` gotcha in project memory.

## 7. Register the source (dev-seed + http scripts)

Sources are seeded at runtime via the REST API, not Flyway (ADR-007 §"Source registration is runtime").

1. **`http/importer/dev-seed.http`** — add a venue-creation `POST /api/admin/venues` (with `district` — the
   frontend has a district filter) capturing the venue id, then a `POST /api/admin/event-sources` linking
   `venueId` + `"sourceType": "<VENUE>"` + the listing `url`, then a `POST /api/admin/event-sources/<slug>/import`
   to trigger the first import. Copy the numbered block format of an existing venue and update the header
   comment/source list at the top. Note the import slug is derived from the source **name**
   (e.g. "Astra Kulturhaus" → `astra-kulturhaus`).
2. Optionally add ad-hoc CRUD examples to `http/importer/event-sources.http` and `http/importer/venues.http`.

No Flyway migration is needed — the `event_source` schema already exists (`V001__create_initial_schema.sql`);
migrations are DDL-only (ADR-005).

## 8. Verify

Run the checks before declaring done:

```bash
./gradlew :events-importer:test --tests '*<Venue>*'   # new tests green
./gradlew :events-importer:ktlintCheck :events-importer:detekt   # style + static analysis
./gradlew :events-importer:test --tests '*ModularityTests'       # Modulith boundaries intact
```

Then run the full pre-PR sequence with `/verify`. The `ModularityTests` check matters: the new package must
stay within the `scraper` module — don't import from other feature modules' internals.

Optionally smoke-test the live site: start the importer (`./gradlew :events-importer:bootRun`) against a fresh
DB and run the new dev-seed block, then check the imported events look sane. Be polite — the per-host throttle
(200ms) applies automatically; don't hammer the venue.

## Checklist

- [ ] `robots.txt` checked; site is server-rendered HTML (or JSON-LD present) — not a JS SPA
- [ ] `EventSource` enum value added with KDoc
- [ ] `<venue>/` package: overview scraper (+ detail scraper if list+detail) + `@Component` importer
- [ ] Shared extension helpers reused; selectors are semantic/structured, not positional
- [ ] `sourceId` is stable and prefixed via `sourceIdPrefix`; events validated before return
- [ ] HTML fixtures saved under `src/test/resources/scraper/<venue>/`
- [ ] Scraper + importer tests covering happy path, edge cases, NotModified, empty page
- [ ] `dev-seed.http` updated (venue + source + trigger); header comment refreshed
- [ ] `ktlintCheck`, `detekt`, `ModularityTests`, and new tests all green; `/verify` clean
