// Explicit `.ts` on every import in this directory: the injector is type-checked under
// `tsconfig.node.json` and bundled by `vite.injector.config.ts`, both of which follow Node's ESM
// resolver — see the note in vite.config.ts.
import { type Locale, LOCALES } from '../src/i18n/locales.ts'

/**
 * Which of the four data-driven route families a request is for, and the two things the injector
 * needs from it: the slug to ask the BFF about, and the locale-relative path the canonical URL is
 * built from.
 *
 * Everything else — static pages, assets, the locale-less root — returns `null` and never reaches
 * the injector at all: nginx only proxies paths this regex also accepts, and the double check is
 * what keeps an nginx edit from widening the injector's surface by accident.
 */

export const ENTITY_KINDS = ['events', 'venues', 'artists', 'promoters'] as const

export type EntityKind = (typeof ENTITY_KINDS)[number]

export interface DetailRoute {
  kind: EntityKind
  locale: Locale
  /** Validated against {@link SLUG}, so it is safe to place in a URL without further escaping. */
  slug: string
  /** Locale-relative, for `canonicalUrl()` and `alternatesFor()` — `/events/<slug>`. */
  path: string
}

/**
 * What a slug may look like. The BFF mints them from dates, names and titles, all lower-cased and
 * hyphenated; anything outside this set is not a slug we ever produced and is refused here rather
 * than forwarded, which is what keeps `..`, `%2e` and friends out of the BFF request.
 */
const SLUG = '[a-z0-9][a-z0-9-]*'

const DETAIL = new RegExp(`^/(${LOCALES.join('|')})/(${ENTITY_KINDS.join('|')})/(${SLUG})/?$`)

/** Parses a request URL. The query string and fragment are ignored, as `seoTags.ts` ignores them. */
export function matchDetailRoute(url: string): DetailRoute | null {
  const pathname = url.split('#')[0]?.split('?')[0] ?? ''
  const match = DETAIL.exec(pathname)
  if (!match) return null

  const [, locale, kind, slug] = match as unknown as [string, Locale, EntityKind, string]
  return { kind, locale, slug, path: `/${kind}/${slug}` }
}
