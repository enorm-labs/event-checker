import { type MaybeRefOrGetter, ref, toValue, watchEffect } from 'vue'

import { HOME_TITLE, type PageMeta } from '@/lib/pageMeta'

/**
 * Writes the current page's title, description and image into the document head.
 *
 * *What* the tags say is decided by `lib/pageMeta.ts`; this only puts them in the DOM. The split
 * is deliberate — the planned meta injector (ADR-014 §Decision 3) needs the former and has no use
 * for the latter, and keeping the composition free of Vue and the DOM is what lets it be shared.
 */

/**
 * The full document title for the current view. Single source of truth shared by the browser tab,
 * the Open Graph tags, and the screen-reader route announcer (see App.vue).
 */
export const pageTitle = ref(HOME_TITLE)

/**
 * The site-level values `index.html` ships with, captured once before anything overwrites them.
 *
 * Without this, a page with no description of its own would keep the *previous* page's — so
 * opening an event and then the imprint would describe the imprint as a club night. Restoring a
 * remembered default is the only way a per-page tag can be un-set again.
 */
const SITE_DEFAULTS = new Map<string, string>()

/** Tags this module owns. `og:image`/`twitter:image` are absent from `index.html` by design. */
const DESCRIPTION_SELECTORS = [
  'meta[name="description"]',
  'meta[property="og:description"]',
  'meta[name="twitter:description"]',
]
const TITLE_SELECTORS = ['meta[property="og:title"]', 'meta[name="twitter:title"]']
const IMAGE_TAGS: [attribute: string, name: string][] = [
  ['property', 'og:image'],
  ['name', 'twitter:image'],
]

function rememberDefault(selector: string): string {
  const existing = SITE_DEFAULTS.get(selector)
  if (existing !== undefined) return existing

  const content = document.head.querySelector<HTMLMetaElement>(selector)?.content ?? ''
  SITE_DEFAULTS.set(selector, content)
  return content
}

/** Sets `content` on an existing `<meta>`, or restores the site default when it is undefined. */
function setMeta(selector: string, content: string | undefined): void {
  const fallback = rememberDefault(selector)
  document.head
    .querySelector<HTMLMetaElement>(selector)
    ?.setAttribute('content', content ?? fallback)
}

/**
 * Applies a page's meta, replacing whatever the previous page left behind.
 *
 * Every tag is written on every call — including back to its default — because the failure mode
 * of a partial update is a page describing itself as the last one you visited, which is worse
 * than a page with no description at all.
 */
export function applyPageMeta(meta: PageMeta): void {
  pageTitle.value = meta.title
  document.title = meta.title
  for (const selector of TITLE_SELECTORS) setMeta(selector, meta.title)
  for (const selector of DESCRIPTION_SELECTORS) setMeta(selector, meta.description)

  // Images are created and removed rather than reset: `index.html` carries no site-level image,
  // so there is no default to fall back to, and leaving an event poster on the imprint would be
  // an outright wrong preview rather than a vague one.
  for (const [attribute, name] of IMAGE_TAGS) {
    const selector = `meta[${attribute}="${name}"]`
    const existing = document.head.querySelector<HTMLMetaElement>(selector)

    if (!meta.image) {
      existing?.remove()
      continue
    }
    if (existing) {
      existing.content = meta.image
      continue
    }
    const element = document.createElement('meta')
    element.setAttribute(attribute, name)
    element.content = meta.image
    document.head.append(element)
  }
}

/**
 * Keeps the head in sync with a reactive per-view meta.
 *
 * Pass a getter: detail views mount before their entity arrives, so the meta is a placeholder
 * first and the real thing once the fetch resolves.
 */
export function usePageMeta(meta: MaybeRefOrGetter<PageMeta>): void {
  watchEffect(() => applyPageMeta(toValue(meta)))
}
