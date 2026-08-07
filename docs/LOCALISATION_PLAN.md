# Plan — Localisation (English + German)

> Status: **Phases 1–3 implemented** (2026-08-07); Phases 4–5 still proposal.
> Delivers Phase 7 of [FOOTER_AND_LEGAL_PLAN.md](FOOTER_AND_LEGAL_PLAN.md) §6.2, which agreed the work and recorded the constraints it places on code written
> since.
> Related: [BRANDING.md](BRANDING.md) · [ADR-010 (styling)](adr/ADR-010_FRONTEND_STYLING_FRAMEWORK.md) · [ADR-011 (calendar)](adr/ADR-011_CALENDAR_LIBRARY.md)

---

## 1. Why this is bigger than it looks

Every previous phase touched a corner of the app. This one touches **every user-facing surface at once**, and three of its decisions are one-way doors — the URL
strategy, what counts as translatable, and where the locale lives. Measured against the current codebase:

|                                          | Count        | Notes                                                         |
|------------------------------------------|--------------|---------------------------------------------------------------|
| `.vue` files with user-facing text       | **20 of 29** | ~145 literal strings                                          |
| TypeScript modules with user-facing text | **7**        | route titles, error messages, date-range presets, page titles |
| e2e assertions bound to English strings  | **~82**      | across 9 spec files                                           |
| Legal pages needing a German version     | **2**        | imprint + privacy, and they are the *hard* ones               |

The legal pages are the reason this cannot be done casually. [FOOTER_AND_LEGAL_PLAN §6.1](FOOTER_AND_LEGAL_PLAN.md) committed to a hard coupling:

> **German legal pages ship in the same release as German UI, not after.** An English-only imprint and privacy notice on a site that presents itself in German
> to a German visitor is indefensible — that is the configuration where the Art. 12 GDPR argument turns against us.

So "add a language switcher" and "translate the privacy notice" are the same deliverable. That is the single most important thing this plan records.

---

## 2. Decisions to make (proposed answers)

These belong in an **ADR-013**, because they are expensive to reverse. Proposed answers below; the ADR is the deliverable of Phase 0.

### 2.1 Library — `vue-i18n` + `@intlify/unplugin-vue-i18n`

Verified against this repository on 2026-08-07:

| Package                      | Latest | Peer requirements                                         | Our version | Verdict |
|------------------------------|--------|-----------------------------------------------------------|-------------|---------|
| `vue-i18n`                   | 11.4.8 | `vue ^3.0.0`, node `>= 22`                                | Vue 3.5.41  | ✅      |
| `@intlify/unplugin-vue-i18n` | 11.2.4 | `vite ^6 \|\| ^7 \|\| ^8`, `vue ^3.2.25`, node `>= 22.13` | Vite 8.2.0  | ✅      |

**One catch worth surfacing now:** `package.json` declares `engines.node: "^20.19.0 || >=22.12.0"`. Both packages require **Node ≥ 22** and the plugin ≥ 22.13,
so adopting them **drops Node 20 support**. That is a deliberate change to make, not a detail to discover in CI.

Note also that vue-i18n's *Legacy API* is deprecated in v11 and removed in v12 — use the **Composition API** (`useI18n`) from the first line, so v12 is a
version bump rather than a migration.

### 2.2 URL strategy — prefixed paths (`/de/events`, `/en/events`)

**Strongly preferred over a stored preference**, and this is the decision that most resists being changed later:

- Each language gets a crawlable URL, which is what makes `hreflang` possible at all.
- A link someone sends is the language they meant to send. A locale kept only in `localStorage` makes every shared URL a coin flip.
- It keeps the § 25 TDDDG posture intact: the URL is the source of truth, and any stored locale is only a redirect hint for the bare `/` — still "strictly
  necessary", still no consent banner. (See FOOTER_AND_LEGAL_PLAN §7.4; the rule there is that *nothing non-essential* may be stored, and a redirect hint for a
  preference the user set is not that.)

Consequences to design for: `/` must redirect to a locale (by `Accept-Language`, falling back to `en`), every existing route gains a prefix, and old unprefixed
URLs should redirect rather than 404 — this site is not live yet, so there are no external inbound links to preserve, which makes now the cheapest possible
moment to do this.

### 2.3 What gets translated — the chrome, not the data

The boundary, made concrete against what is actually in the codebase:

|                                                    | Translate?    | Why                                                                                                                                              |
|----------------------------------------------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| UI labels, headings, empty and error states        | ✅            | The chrome                                                                                                                                       |
| Legal pages (imprint, privacy)                     | ✅            | Mandatory — §6.1                                                                                                                                 |
| Event titles, venue names, artist names, line-ups  | ❌            | Third-party content. Translating "Berghain" or a band name would be wrong, not merely unnecessary                                                |
| **Berlin district names** (`src/lib/districts.ts`) | ❌            | Proper nouns. *Mitte* is *Mitte* in every language. This file looks translatable and is not — the trap this table exists to flag                 |
| **Event types** (`CONCERT`, `CLUB_NIGHT`, …)       | ✅            | Enum-backed, so they are ours. But see §2.4 — the current mechanism cannot do it                                                                 |
| Genre tags                                         | ⚠️ **Decide** | Enum-ish but sourced from venues. "Techno" is "Techno"; "Singer-Songwriter" is too. Recommendation: **do not translate** — they behave like data |
| Brand name and tagline                             | ⚠️ **Decide** | "Event Junkie" stays. *"Can't get enough of Berlin"* needs a German register, not a literal rendering — see §5                                   |

### 2.4 Two formatting functions that must change, and one that must not

`src/lib/format.ts` is where locale leaks into logic today:

- **`formatDate`** hardcodes `Intl.DateTimeFormat('en-GB', …)` → must take the active locale. This is the main visible change: *Fri, 12 Jun 2026* becomes *Fr.,
  12. Juni 2026*.
- **`formatEventType`** derives its label by string manipulation (`CLUB_NIGHT` → `Club night`). **This cannot be translated by any amount of locale plumbing** —
  it must become a message lookup keyed by the enum value, with a fallback for `OTHER` and for values the frontend has not seen. Ten values, listed in
  `events-core`'s `EventType`.
- **`todayIso` uses `Intl.DateTimeFormat('en-CA')` as a trick to get `YYYY-MM-DD`** — that is a *format*, not a language. **Do not make it locale-aware.**
  Anyone doing a find-and-replace on locale strings will break every date filter in the app, silently, because the output stays a plausible date.

**`formatPrice` already uses `de-DE`** (`38,00 €`) regardless of UI language. Recommendation: **leave it German in both locales.** That is the price written on
the door in Berlin, and `€38.00` would be a worse answer for an English-speaking user standing at that door. Worth a deliberate line in the ADR rather than
letting it drift.

---

## 3. Where the strings are

Rough distribution, to size the extraction rather than to be exact:

```
19  components/EventFilterBar.vue      16  views/legal/PrivacyView.vue
14  views/EventDetailView.vue          12  App.vue
11  components/AppFooter.vue           11  views/VenuesView.vue
11  views/legal/ImprintView.vue         9  views/HomeView.vue
 7  views/AboutView.vue                 7  views/EventsView.vue
 7  views/legal/NoticesView.vue         6  components/EventCard.vue
```

Plus, in TypeScript: `router/index.ts` (route titles), `api/client.ts` (four error messages), `lib/dateRanges.ts` (preset labels), `composables/usePageTitle.ts`
(brand + tagline), `lib/format.ts` (§2.4).

**Message file layout:** one file per locale, split by feature (`en/common.json`, `en/events.json`, `en/legal.json`, …) rather than one flat file. The legal
namespace alone is several hundred lines of prose and would otherwise dominate every diff.

---

## 4. Testing — the part most likely to be underestimated

~82 e2e assertions address elements by their English accessible name. That is *by design* — it is what makes the Playwright suite double as an accessibility
regression test — but it means the naive approach (run everything twice, once per locale) roughly doubles a suite that already takes ~1.3 minutes across five
browser projects.

**Recommendation:**

- **Pin the existing suites to `/en`.** They are behaviour tests that happen to use English as a stable handle; re-asserting the same behaviour in German buys
  little and costs a lot.
- **Add one focused `e2e/i18n.spec.ts`** covering what is genuinely locale-specific: the switcher changes the URL prefix, a German URL renders German, `<html
  lang>` follows, dates render in the German format, and the German legal pages exist and are reachable.
- **Extend the axe sweep to the German routes.** Text length changes layout, and German is reliably longer — this is where an overflow or a contrast regression
  will actually appear.
- **Add a unit test asserting the two message catalogues have identical key sets.** A missing German key silently falls back to English, which is precisely the
  failure that ships unnoticed.

---

## 5. Copy that needs a decision, not a translation

Three pieces cannot be handed to a translator as-is:

- **The tagline.** *"Can't get enough of Berlin"* is a pun on the brand's premise (BRANDING.md §2). A literal German rendering loses it. This needs a German
  line written *from the concept*, and BRANDING.md should record both as equals rather than treating German as a translation of English.
- **The disclaimer.** FOOTER_AND_LEGAL_PLAN §7.6 already drafts both: *"Alle Angaben ohne Gewähr"* and the English version — deliberately not literal renderings
  of each other. Use those, do not re-translate.
- **The beta explanation** on the About page, which is written in the brand voice rather than as neutral prose.

---

## 6. The legal pages

The largest single piece of writing in this phase, and the one with an actual legal standard attached.

- **German becomes the authoritative version** once both exist, stated in one sentence on each page: *"Maßgeblich ist die deutsche Fassung."* The controller,
  the venue and the supervisory authority are all German (FOOTER_AND_LEGAL_PLAN §6.1).
- The imprint's § 5 DDG headings have conventional German wording — *Angaben gemäß § 5 DDG*, *Haftung für Inhalte*, *Haftung für Links* — and should use it.
  Inventing phrasing here reads as a translated foreign document, which is the impression a German imprint most needs to avoid.
- **This is the point at which the DSGVO generator (FOOTER_AND_LEGAL_PLAN §7.8) earns its keep**: it emits both languages from one set of inputs, and the German
  it produces is the idiom a German reader expects. Run it as a cross-check against the hand-written German before shipping.
- The provisional banner (`ProvisionalNotice.vue`) and the placeholder-tripwire test both need German copy. The tripwire asserts on the *address*, not on prose,
  so it survives — but check it rather than assume.

---

## 7. Proposed phases

Sequenced so nothing user-visible ships half-translated.

**Phase 0 — ADR-013.** ✅ Written: [ADR-013](adr/ADR-013_LOCALISATION.md), status *Proposed*. It settles the library, the URL strategy, the
translate-the-chrome boundary, the four formatting rules, and which version of the legal pages is authoritative. Three of §8's open questions are answered there
(genre tags → data; prices → stay `de-DE`; default locale → `Accept-Language` falling back to `en`); the German tagline is deferred to BRANDING.md as a brand
rather than architectural decision. **Blocks everything below until the status moves to Accepted.**

**Phase 1 — plumbing.** ✅ done (2026-08-07). Three things differed from the sketch below:
> - **Only `en` is published.** `LOCALES` lists what exists, and the route matcher is built from it, so `/de/*` is not routable until German messages ship.
>   Declaring `de` early would have made `/de/events` render English — the half-state this plan warned about, arriving through the routing layer instead of the
>   catalogue.
> - **It is not quite "no visible change": URLs move** to `/en/…`. Content is untouched, but every link, bookmark and test URL gains a prefix.
> - **`formatEventType` became `humaniseEventType`** and moved behind `useFormat()`, which prefers the catalogue and falls back to sentence-casing for enum
>   values the frontend has not seen. The BFF's `EventType` can gain a value in a release that ships before the frontend.
>
> A trailing-slash bug surfaced and was fixed: `/` redirected to `/en/` while every in-app link produced `/en` — two URLs for one page.
>
> Original scope: Install `vue-i18n` + the plugin, bump `engines.node`, add the locale files with **English only**, wire `useI18n`,
make `formatDate` locale-aware and convert `formatEventType` to a lookup. Route prefixes and the redirect from `/`. Reactive `<html lang>`. At the end of this
phase the site looks identical and only English exists — which is exactly what makes it safe to review.

**Phase 2 — extraction.** ✅ done (2026-08-07). **One decision below turned out to be wrong and was changed.**
>
> The plan assumed the legal pages would become an `en/legal.json` namespace, "several hundred lines of prose". Measured, the About page and the three legal
> pages carry **~1,600 words across 49 paragraphs, with 29 inline links, `<strong>` and `<code>` elements *inside* those paragraphs**. Every way of putting that
> in JSON is bad: HTML inside strings rendered with `v-html`; `<i18n-t>` component interpolation at 29 sites; or shattering sentences into fragments no
> translator could work from.
>
> **So long-form prose stays in components, and those pages get a per-locale component in Phase 4.** Only their *chrome* is extracted — the review-date label
> and the provisional banner heading. Everything else on those four pages is untouched.
>
> Extracted: **11 namespace files**, every string in the app chrome, plus the TypeScript side — route titles became `titleKey` message keys, date presets carry
> a key instead of an English label, and `describeError` now takes a subject *key* and translates through the global i18n instance (it runs in an async `catch`,
> long after any setup context). The brand name "Event Junkie" is deliberately left as a literal in `BrandLogo` and the home hero — it is not translated.
>
> The key-parity test landed with it (`src/i18n/__tests__/messages.spec.ts`): identical key sets, no empty strings, matching `{named}` placeholders, and a
> catalogue for every published locale. It compares English against itself for now, which is the point — parity is enforced *before* German arrives rather than
> remembered afterwards.
>
> Original scope: Move all ~145 template strings and the TypeScript strings into the English catalogue. Mechanical, large, and best reviewed as its own
diff with no behaviour change. The key-parity test lands here, comparing English against itself until German arrives.

**Phase 3 — German UI.** ✅ done (2026-08-07). German is published: `/de/*` is live, `Accept-Language` resolves to it, and the footer carries a locale switcher.
>
> **The legal pages are still English**, which is the state §6.1 rules out *at go-live*. Nothing is deployed, so this is safe on `main` — but it is disclosed on
> the page itself rather than left for a German reader to find: the provisional banner gains a line, in German, saying the page is English-only for now and that
> the German version will be the authoritative one. It disappears by itself when Phase 4 lands.
>
> **Two bugs surfaced, one of them mine from Phase 1.** `formatDate` was switched to take the active locale and given the bare tag `en` — which `Intl` resolves
> to US conventions, so English dates had silently become "Jun 12, 2026" instead of "12 Jun 2026". A UI locale is not a formatting locale; `INTL_LOCALES` now
> maps `en → en-GB` and `de → de-DE`, with a unit test pinning the order. The key-parity test from Phase 2 was also comparing *precompiled message ASTs* rather
> than source strings — harmless while comparing English to itself, useless the moment a second language existed. It now reads the JSON from disk.
>
> Original scope: Write `de` messages for everything except the legal pages. Add the footer locale switcher. Extend the axe sweep to `/de`.

**Phase 4 — German legal pages + go-live coupling.** Imprint and privacy in German, the authoritative-version sentence, the generator cross-check, BRANDING.md's
German register. **Phases 3 and 4 must be released together** (§1).

**Phase 5 — SEO.** `hreflang` alternates, per-locale `og:locale`, and the sitemap. This compounds with the existing prerendering item: a client-rendered SPA
whose language is only resolved in JavaScript is worse for crawlers than one with no translations at all.

---

## 8. Open questions

1. **Genre tags** — translate, or treat as data? (§2.3. Recommendation: data.)
2. **Prices** — keep `de-DE` formatting in both locales? (§2.4. Recommendation: yes.)
3. **Default locale for a visitor with no preference** — `Accept-Language`, or always `/en`? Recommendation: honour `Accept-Language`, fall back to `en`.
4. **Does `engines.node` dropping Node 20 affect anything else?** Nothing in CI pins 20, but worth confirming before it becomes a surprise.
5. **Is a German tagline wanted at all**, or does the brand stay English even on the German site? Legitimate either way — many Berlin brands do the latter.

---

## 9. What this plan does not cover

- More than two languages. The structure supports it; nothing here assumes it.
- Translating event data, venue descriptions, or anything sourced from third parties (§2.3).
- Server-side rendering or prerendering, which SEO ultimately wants and which is tracked separately.
- Localising the backend. The BFF returns data and RFC 9457 problem details; the frontend owns all user-facing language. If that ever changes, `Accept-Language`
  handling in the BFF becomes a separate decision.
