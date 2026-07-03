# Branding — Event Junkie

> Status: **living document** (started 2026-07-02). The brand foundation (name, tagline, voice) is
> decided; the **logo** and **visual-design** sections are *ideas to explore*, not committed decisions.
> Related: [VISION_ROADMAP_IDEAS.md](VISION_ROADMAP_IDEAS.md) · [ADR-010 (styling framework)](adr/ADR-010_FRONTEND_STYLING_FRAMEWORK.md).

## 1. Brand foundation

|                          |                                                                                                                        |
|--------------------------|------------------------------------------------------------------------------------------------------------------------|
| **Public name**          | **Event Junkie** (domain: `event-junkie.de`)                                                                           |
| **Internal / repo name** | **Event Checker** (repo, modules, READMEs, ADRs — see the naming rule below)                                           |
| **Tagline**              | *Can't get enough of Berlin*                                                                                           |
| **One-liner**            | Your always-fresh feed of what's on across Berlin's venues — concerts, club nights, festivals, and the odd quiz night. |
| **Scope**                | All music, every genre and room size — not just techno. Berlin-only for now.                                           |

### Naming rule

- **Public / user-facing surfaces** (page titles, home hero, About copy, OG tags, the eventual domain and
  marketing) use **Event Junkie**.
- **Internal / technical surfaces** (repository name `event-checker`, Gradle modules, package identifiers,
  DB schema, ADRs, developer docs) stay **Event Checker**. Keeping an internal codename distinct from the
  public brand is deliberate — don't "fix" these to Event Junkie.

## 2. The concept — why "Junkie"

The name works because a junkie's traits map cleanly onto what the product does:

| Junkie trait                             | Product truth                                                               |
|------------------------------------------|-----------------------------------------------------------------------------|
| Always chasing the next **hit**          | A "hit" is both a drug hit *and* a music hit — every event is the next one. |
| Always knows where to **score**          | The app *is* the source: the one place that always knows what's on.         |
| Wired into the scene, ahead of the crowd | An always-fresh feed so you know before it sells out.                       |
| Feeds a **habit**, comes back nightly    | Discovery you return to; you never come up dry.                             |

**Metaphor to lean on:** the user is the *junkie*; the app is quietly the *dealer/source*. Name the
audience (Junkie); let "source / score / hit / fix / feed the habit" show up in the *copy*. Words that carry
the double meaning — **hit**, **score** — are the strongest.

## 3. Voice & tone

Playful, self-aware, a little nocturnal — never actually about drugs. It flatters the user ("you can't get
enough") rather than the app. Confident and in-the-know, but warm, not edgy-for-its-own-sake.

**Do:** short, punchy, wink-y; nightlife/music vocabulary; treat FOMO as the enemy.
**Don't:** glorify substance abuse, be crude, or over-explain the joke. Keep it PG-13 and inclusive.

Great places to let the voice show — **microcopy**:

- Empty state: *"Nothing on tonight? In Berlin? Unlikely — try a wider date range."*
- End of list: *"That's the lot. Go touch some grass (or don't)."*
- 404 / not found: *"This one's gone. Like last call — you snooze, you lose."*
- Loading: *"Scoring the latest…"*

Tagline alternatives explored (kept for reference / A-B testing): *Never miss a hit* · *Highly addictive* ·
*Feed the habit* · *Your dealer for Berlin nightlife* · *Know before the crowd*.

## 4. Logo — directions to explore

**Done:** direction #1 below (pulse / waveform wordmark) was prototyped and shipped — the pulse mark is the
favicon (`events-frontend/public/favicon.svg`) and, paired with the wordmark, the header lockup
(`src/components/BrandLogo.vue`, collapsing to just the mark on mobile). The other directions stay parked as
alternatives. The principles that guided it:

- **Monochrome-first.** The UI theme is currently all-grayscale; the mark must read in a single ink and
  invert cleanly for dark mode. Design in black/white, add the accent (§5) as a highlight only.
- **Favicon-legible.** It has to survive at 16–32 px and as an emoji-style tab/app icon. Favour one strong
  silhouette.
- **Ship as SVG**, inline-able (the artifact/title system and CSP prefer self-contained assets).

Candidate directions (ordered by how well they fuse *music + the "junkie" concept*):

1. **Pulse / waveform wordmark** *(recommended lead).* "Event Junkie" set in the site font, with a small
   ECG-heartbeat / audio-waveform line replacing the crossbar of a letter or underlining the word. Fuses
   **heartbeat + music waveform + "never miss a beat" + addiction**. The waveform alone becomes the favicon.
2. **"EJ" monogram.** A tight ligature of E + J for the app icon / favicon; pairs with the wordmark for
   full-lockup use.
3. **Pin + play.** A Berlin map-pin whose "hole" is a play triangle or a music note — literally "events at
   venues." Very legible small; a touch more literal / less witty.
4. **Wristband / ticket stub.** A club wristband or torn ticket — instant "nightlife entry." Characterful,
   but busier at favicon size.
5. **The live dot.** A single filled circle — a "hit," a record, a dot on a calendar day — that **pulses**
   when something's on tonight. Minimal, animatable, unbeatable as a favicon; leans on motion for meaning.

**Shipped:** #1 (waveform wordmark) + its standalone favicon glyph, in both inks.

## 5. Website / visual design — ideas

Grounded in the real stack: **Tailwind CSS v4 + shadcn-vue**, **Geist** type, **oklch** CSS-variable tokens
in `events-frontend/src/assets/main.css`, dark mode via the `.dark` class (see ADR-010). Re-theming is a
token edit, so most of the below is low-cost to try.

### 5.1 Colour — introduce ONE electric accent

**Done:** the **UV violet** row below was applied to `--primary` / `--accent` / `--ring` (and the matching
sidebar tokens), keeping everything else neutral so the accent reads like a spotlight in a dark room —
AA-verified in both modes. The other rows are kept as alternatives. Candidate accents (drop-in oklch):

| Direction              | Vibe                         | Light `--primary`      | Dark `--primary`       |
|------------------------|------------------------------|------------------------|------------------------|
| **UV violet** *(rec.)* | Club blacklight, after-hours | `oklch(0.55 0.24 295)` | `oklch(0.72 0.20 295)` |
| Electric magenta       | Neon, flyer-pink             | `oklch(0.60 0.25 350)` | `oklch(0.72 0.21 350)` |
| Acid green             | Rave, high-energy            | `oklch(0.72 0.22 150)` | `oklch(0.80 0.20 150)` |
| Berlin red             | Bold, editorial              | `oklch(0.58 0.22 25)`  | `oklch(0.70 0.19 25)`  |

Notes: keep `--background`, `--card`, `--muted`, borders neutral. Verify **WCAG AA** contrast for text on
accent and accent on background in *both* modes (accessibility is a first-class project value). The
existing red `--destructive` must stay visually distinct from any red-ish accent — favours violet/magenta.

### 5.2 Dark-mode-first

Nightlife skews dark. **Done:** dark mode is now the **default for first-time visitors**, set by the
pre-paint script in `index.html` so there's no flash. An explicit light choice is remembered in
`localStorage` and always wins on later visits. The default is unconditional (not gated on
`prefers-color-scheme`) — a deliberate brand call; revisit if it proves user-hostile for light-OS users.
The accent is tuned to glow on the dark surface.

### 5.3 Typography

- **Body / UI:** **Geist** — now actually rendering and **self-hosted** via `@fontsource-variable/geist`
  (imported in `main.ts`); the render-blocking Google Fonts request is gone. (A shadcn-scaffold name
  mismatch had it silently falling back to a system font until that was fixed.)
- **Display / hero:** *(open)* consider a characterful face for big headings (a tight grotesque, or a mono
  for a "listings/terminal" edge) to add nightlife personality; keep Geist for everything functional.

### 5.4 Imagery

Event/venue photos are the hero content but come from many scraped sources, so they clash. Apply a
**consistent treatment** — grayscale or a duotone tinted with the brand accent on cards, revealing full
colour on hover / detail pages. Cohesive look, and it makes the accent do double duty.

### 5.5 Motion (subtle)

`tw-animate-css` is available. Ideas: a gently **pulsing "live tonight" dot**, soft card hover-lift, a
waveform that animates on the logo. Always gate behind `prefers-reduced-motion: reduce`.

### 5.6 Page-level notes

- **Home:** lead with *tonight / this week* — the "next fix." Hero = wordmark + tagline (already the title).
- **Events:** filter-forward (genre/type already exist); make "what's on this weekend" a one-tap default.
- **Calendar:** the signature screen (ADR-011) — brand the "has events" day markers with the accent.
- **Detail pages:** editorial layout; big image, lineup, venue — the place to reveal full-colour imagery.
- **Empty/404/loading:** carry the §3 voice.

## 6. How this maps to code

- **Colour / radius / type tokens** → `events-frontend/src/assets/main.css` (`:root` + `.dark`). Re-theming
  is CSS-variable edits only (ADR-010).
- **Page titles & tagline** → already implemented in `src/composables/usePageTitle.ts`
  (`APP_NAME`, `TAGLINE`, `HOME_TITLE`) and `index.html` (title + OG/Twitter tags).
- **Favicon / logo** → `events-frontend/public/favicon.svg` (pulse badge) and
  `src/components/BrandLogo.vue` (header lockup).
- **Fonts** → self-hosted `@fontsource-variable/geist`, imported in `src/main.ts`; `--font-*` tokens in
  `main.css`.

## 7. Open questions / next steps

- [x] Applied the **UV violet** accent to the tokens, AA-verified in both modes (§5.1).
- [x] Prototyped the waveform wordmark and shipped it as the favicon + header lockup (§4).
- [x] Dark mode is the default for new visitors (§5.2).
- [x] Self-hosted Geist via `@fontsource-variable/geist`, so it actually renders with no external request (§5.3).
- [ ] Decide on a display/hero type face vs. staying all-Geist (§5.3).
- [ ] Add an `apple-touch-icon` PNG (iOS home screen doesn't render SVG favicons).
- [ ] Register `event-junkie.de` (tracked in the roadmap).

### Design refresh — applying the prototype look app-wide

A sequence that also captures the §3–§5 design ideas not tracked in the checklist above.

- [x] Home hero — ambient violet glow, animated pulse mark, wordmark + tagline — and mono eyebrow
  section labels (`PulseMark`, `SectionLabel`, motion keyframes in `main.css`). *(§5.5, §5.6)*
- [x] Refined event cards + a pulsing "live tonight" dot + hover-lift, gated by reduced-motion. *(§5.5)*
- [ ] Events & Calendar: eyebrow headers, filter-forward polish, accent-branded day markers. *(§5.6)*
- [ ] Detail pages: editorial layout + eyebrow section labels; duotone/accent image treatment. *(§4, §5.4)*
- [ ] Empty / 404 / loading microcopy in the brand voice. *(§3)*

## Glossary

Shorthand used across this doc, the code, and PR descriptions. *(planned)* marks a term whose
implementation is still on the backlog above.

- **Accent** — the single brand hue (UV violet) applied to the `--primary` / `--accent` / `--ring`
  tokens; everything else stays neutral so it reads like a spotlight. See §5.1.
- **Ambient glow** — the soft radial violet light in the home hero, centered on the pulse mark so the
  mark reads as its source. Fades to transparent on its own (no `overflow` clip), never a hard shape.
- **Duotone** — a two-tone image treatment (shadows → one colour, highlights → another) to make
  mismatched, scraped event photos feel cohesive. See §5.4. *(planned)*
- **Eyebrow label** — a small, mono, uppercase, letter-spaced heading in the accent, used where a
  section title goes (e.g. "TONIGHT"). The editorial "listings" look. Component: `SectionLabel.vue`.
- **Favicon badge** — the app icon: a rounded, violet-gradient square holding the white pulse.
  File: `events-frontend/public/favicon.svg`.
- **Home hero** — the top block of the home page: the animated pulse mark, the wordmark, the tagline,
  and the primary call-to-action, over the ambient glow.
- **Live dot** — a small pulsing accent dot on cards for events happening today, reinforcing liveness.
  See §5.5; implemented in `EventCard.vue`.
- **Lockup** — the mark and wordmark used together as one unit (e.g. in the header nav).
  Component: `BrandLogo.vue`.
- **Monogram** — an "EJ" ligature; a parked logo alternative for icon-only use. See §4.
- **oklch** — the perceptual colour space the theme tokens are written in:
  `oklch(lightness chroma hue)`. Neutral tokens have chroma `0`.
- **Pulse mark** — the logomark itself: a single-stroke line that is at once a soundwave, a heartbeat
  (ECG), and a "hit". Component: `PulseMark.vue`; also the favicon glyph.
- **Reversed / single-ink** — the mark or lockup drawn in one flat colour (e.g. white on the accent),
  for contexts where the gradient can't render.
- **Token** — a CSS-variable design value (colour, radius, font) in `main.css` (`:root` + `.dark`);
  re-theming means editing tokens, not components. See §6.
- **Wordmark** — "Event Junkie" set as type (accent on "Junkie"), as distinct from the pulse mark.
  Part of the lockup and the hero.
