import { type MaybeRefOrGetter, onScopeDispose, toValue, watchEffect } from 'vue'

import type { JsonLd } from '@/lib/structuredData'

/**
 * Publishes JSON-LD documents into a `<script type="application/ld+json">` for the current view.
 *
 * Pass a getter. Detail views load their entity **after** mounting, so the documents are null on
 * first render and arrive later; `watchEffect` republishes when they do. Navigating between two
 * events keeps the same component mounted and only changes the slug, which is the case that would
 * otherwise leave the previous event's data describing the new page.
 *
 * The element is removed when the effect scope is disposed, so nothing leaks across routes.
 */
export function useStructuredData(documents: MaybeRefOrGetter<JsonLd | JsonLd[] | null>): void {
  let script: HTMLScriptElement | null = null

  const remove = () => {
    script?.remove()
    script = null
  }

  watchEffect(() => {
    const value = toValue(documents)
    const list = (Array.isArray(value) ? value : [value]).filter(Boolean) as JsonLd[]

    if (!list.length) return remove()

    script ??= document.head.appendChild(
      Object.assign(document.createElement('script'), { type: 'application/ld+json' }),
    )
    // JSON-LD holds event titles and descriptions scraped from venue websites — third-party text
    // this project does not control. Setting `textContent` keeps the browser from parsing it as
    // markup, and escaping `<` covers the serialisation paths that would not: a title containing
    // `</script>` must not be able to close the element it lives in.
    script.textContent = JSON.stringify(list.length === 1 ? list[0] : list).replace(/</g, '\\u003c')
  })

  onScopeDispose(remove)
}
