/**
 * The locales this site is published in, and how one is chosen.
 *
 * Kept separate from `i18n/index.ts` so the router can import it without pulling in vue-i18n and
 * the message catalogues — the router runs before the app is created.
 *
 * See docs/adr/ADR-013_LOCALISATION.md.
 */

/**
 * The locales this site actually publishes. The first is the fallback.
 *
 * Listing a locale here makes `/<locale>/*` routable **and** makes it resolvable from
 * `Accept-Language` — so a German-speaking visitor lands on `/de` from that moment on. It drives
 * the route matcher directly (see `router/index.ts`), so the two cannot drift apart, and the
 * key-parity test fails if a published locale has no message catalogue.
 *
 * **Before go-live, every published locale needs its legal pages in that language** — an
 * English-only imprint and privacy notice on a site presenting itself in German is the one
 * configuration FOOTER_AND_LEGAL_PLAN §6.1 rules out. German legal pages are Phase 4; until they
 * exist, those pages carry a visible notice saying so.
 */
export const LOCALES = ['en', 'de'] as const

export type Locale = (typeof LOCALES)[number]

export const DEFAULT_LOCALE: Locale = 'en'

/**
 * `localStorage` key holding the visitor's last locale.
 *
 * This is a **hint for resolving a bare `/` only** — the URL is always the source of truth
 * (ADR-013 §2). Keeping the choice out of the URL entirely would make every shared link a coin
 * flip for the recipient, and it is also what keeps the § 25 (2) 2 TDDDG position intact: a
 * preference the visitor set themselves is strictly necessary, so no consent banner. Do not start
 * storing anything else here — see the privacy rules in AGENTS.md.
 */
export const LOCALE_STORAGE_KEY = 'locale'

/**
 * The BCP-47 tag each UI locale formats dates and numbers with.
 *
 * A UI locale is not a formatting locale. Bare `en` resolves to US conventions in `Intl`, so dates
 * come out "Jun 12, 2026" — month first, which is wrong for a Berlin audience reading a European
 * site. `en-GB` gives "12 Jun 2026". This mapping was added after Phase 1 changed `formatDate` to
 * take the active locale and silently switched the English format to US ordering.
 */
export const INTL_LOCALES: Record<Locale, string> = {
  en: 'en-GB',
  de: 'de-DE',
}

export function isLocale(value: unknown): value is Locale {
  return typeof value === 'string' && (LOCALES as readonly string[]).includes(value)
}

/** The stored preference, if it is still a locale we publish. */
function storedLocale(): Locale | null {
  try {
    const stored = localStorage.getItem(LOCALE_STORAGE_KEY)
    return isLocale(stored) ? stored : null
  } catch {
    // Private mode and blocked storage: fall through to the browser's languages.
    return null
  }
}

/** The best match from `Accept-Language`, as the browser reports it. */
function browserLocale(): Locale | null {
  for (const tag of navigator.languages ?? []) {
    // `de-AT` and `de-CH` should both get German.
    const base = tag.split('-')[0]?.toLowerCase()
    if (isLocale(base)) return base
  }
  return null
}

/**
 * Which locale to send a visitor to when the URL does not say — i.e. a bare `/` or an unprefixed
 * path. Their own previous choice wins over what their browser asks for; `en` is the last resort.
 */
export function resolveLocale(): Locale {
  return storedLocale() ?? browserLocale() ?? DEFAULT_LOCALE
}

/** Remembers the choice for the next bare-`/` visit. Best-effort; storage can be unavailable. */
export function rememberLocale(locale: Locale): void {
  try {
    localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  } catch {
    // Ignore — the URL still carries the locale, so nothing is lost within this visit.
  }
}
