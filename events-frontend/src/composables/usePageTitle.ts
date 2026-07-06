import { type MaybeRefOrGetter, ref, toValue, watchEffect } from 'vue'

/** Brand name shown in the browser tab, appended to every interior view's title. */
export const APP_NAME = 'Event Junkie'

/** Homepage tagline — the descriptor best practice recommends over a bare brand name. */
export const TAGLINE = "Can't get enough of Berlin"

/** The root/home title: brand plus tagline. Interior views use `<page> · Event Junkie`. */
export const HOME_TITLE = `${APP_NAME} — ${TAGLINE}`

/**
 * The full document title for the current view. Single source of truth shared by the
 * browser tab, the Open Graph tags, and the screen-reader route announcer (see App.vue).
 */
export const pageTitle = ref(HOME_TITLE)

/** Formats an interior page title as `<page> · Event Junkie`; falls back to the home title. */
export function formatTitle(title?: string | null): string {
  return title ? `${title} · ${APP_NAME}` : HOME_TITLE
}

/** Reflects `content` into an existing `<meta>` tag, matched by name or property. */
function setMeta(selector: string, content: string): void {
  document.head.querySelector<HTMLMetaElement>(selector)?.setAttribute('content', content)
}

/**
 * Sets the already-formatted title as the current page title, keeping the browser tab
 * and the social-share `og:title`/`twitter:title` tags in sync. Note: updating OG tags
 * client-side only reaches JS-executing crawlers — full coverage needs SSR/prerender.
 */
export function setPageTitle(title: string): void {
  pageTitle.value = title
  document.title = title
  setMeta('meta[property="og:title"]', title)
  setMeta('meta[name="twitter:title"]', title)
}

/**
 * Keeps the page title in sync with a reactive per-view title. Pass a getter so titles
 * that depend on async-loaded data update once it arrives.
 */
export function usePageTitle(title: MaybeRefOrGetter<string | null | undefined>): void {
  watchEffect(() => setPageTitle(formatTitle(toValue(title))))
}
