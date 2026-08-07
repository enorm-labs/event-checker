import { useRoute } from 'vue-router'

import { isLocale, type Locale, resolveLocale } from '@/i18n/locales'

/**
 * Prefixes an in-app path with the active locale: `/events` → `/en/events`.
 *
 * Every internal link needs this. Routes are locale-prefixed (ADR-013 §Decision 2), so a bare
 * `to="/events"` would fall through to the catch-all redirect — it would still *work*, but every
 * navigation would cost a redirect and the intermediate URL would briefly be wrong.
 *
 * Deliberately reads `route.params` rather than importing from the router module: the router
 * imports the views, the views import this, and closing that loop is how a circular import
 * becomes an undefined-at-module-init bug.
 */
export function useLocalePath() {
  // `useRoute()` yields undefined when no router is installed. Component unit tests mount without
  // one and stub `RouterLink`, and a link helper is not a good reason to make every card test
  // build a router — so fall back rather than throw. In the app a route is always present.
  const route = useRoute() as ReturnType<typeof useRoute> | undefined

  return function localePath(path: string): string {
    const fromUrl = route?.params?.locale
    const locale: Locale = isLocale(fromUrl) ? fromUrl : resolveLocale()
    // `/` is the locale root itself — `/en/` would be a second, uglier URL for the same page.
    return path === '/' ? `/${locale}` : `/${locale}${path}`
  }
}
