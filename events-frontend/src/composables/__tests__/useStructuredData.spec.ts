import { beforeEach, describe, expect, it } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount } from '@vue/test-utils'

import { useStructuredData } from '@/composables/useStructuredData'
import type { JsonLd } from '@/lib/structuredData'

/**
 * The lifecycle half of structured data — which is where the bugs are, because the documents
 * themselves are pure functions and the DOM is not.
 */

const scripts = () => [...document.head.querySelectorAll('script[type="application/ld+json"]')]
const payload = () => JSON.parse(scripts()[0]?.textContent?.replace(/\\u003c/g, '<') ?? 'null')

function mountWith(source: () => JsonLd | JsonLd[] | null) {
  return mount(
    defineComponent({
      setup() {
        useStructuredData(source)
        return () => h('div')
      },
    }),
  )
}

beforeEach(() => {
  document.head.innerHTML = ''
})

describe('useStructuredData', () => {
  it('publishes one ld+json script', () => {
    mountWith(() => ({ '@type': 'Event', name: 'Test' }))

    expect(scripts()).toHaveLength(1)
    expect(payload()).toEqual({ '@type': 'Event', name: 'Test' })
  })

  it('publishes several documents as a single JSON array', () => {
    mountWith(() => [{ '@type': 'Event' }, { '@type': 'BreadcrumbList' }])

    expect(scripts()).toHaveLength(1)
    expect(payload()).toEqual([{ '@type': 'Event' }, { '@type': 'BreadcrumbList' }])
  })

  it('writes nothing at all while the entity is still loading', () => {
    // Detail views mount before their data arrives. An empty or half-built document is worse than
    // no document: Google would read it, reject it, and have spent the crawl.
    mountWith(() => null)

    expect(scripts()).toEqual([])
  })

  it('publishes once the data arrives, and again when it changes', async () => {
    const name = ref<string | null>(null)
    mountWith(() => (name.value ? { '@type': 'Event', name: name.value } : null))
    expect(scripts()).toEqual([])

    name.value = 'First'
    await Promise.resolve()
    expect(payload()).toMatchObject({ name: 'First' })

    // Navigating between two events keeps this component mounted and only swaps the slug — the
    // case that would otherwise leave the previous event describing the new page.
    name.value = 'Second'
    await Promise.resolve()
    expect(scripts()).toHaveLength(1)
    expect(payload()).toMatchObject({ name: 'Second' })
  })

  it('cannot be broken out of by scraped third-party text', () => {
    // Event titles and descriptions come from venue websites. A title containing `</script>` must
    // not be able to close the element it lives in.
    mountWith(() => ({ '@type': 'Event', name: '</script><img src=x onerror=alert(1)>' }))

    const raw = scripts()[0]!.textContent!
    expect(raw).not.toContain('</script>')
    expect(raw).toContain('\\u003c')
    // Still valid JSON-LD once parsed — escaping must not corrupt the data.
    expect(payload().name).toBe('</script><img src=x onerror=alert(1)>')
  })

  it('removes its script when the view goes away', async () => {
    const wrapper = mountWith(() => ({ '@type': 'Event' }))
    expect(scripts()).toHaveLength(1)

    wrapper.unmount()

    expect(scripts()).toEqual([])
  })
})
