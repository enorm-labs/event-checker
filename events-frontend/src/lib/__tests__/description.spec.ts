import { describe, expect, it } from 'vitest'

import type { EventDetail } from '@/api/types'
import { descriptionFor } from '@/lib/description'

/**
 * Which text a page shows, and what language it declares for it.
 *
 * The failure this guards is not a blank page: it is a page that shows German prose and tells a
 * screen reader, a crawler and the browser's translate feature that it is English. That was true
 * of every event page before ADR-026, and it is invisible without an assertion.
 */
const event = (fields: Partial<EventDetail> = {}): EventDetail =>
  ({
    slug: '2026-06-12-lido-test-act',
    title: 'Test Act',
    ...fields,
  }) as EventDetail

describe('descriptionFor', () => {
  it('shows the original when it is already in the visitor’s language', () => {
    const chosen = descriptionFor(
      event({ description: 'Ein Abend mit Aussicht', descriptionLanguage: 'de' }),
      'de',
    )

    expect(chosen).toEqual({ text: 'Ein Abend mit Aussicht', lang: 'de', machine: false })
  })

  it('prefers the other-language text when the visitor reads that language', () => {
    const chosen = descriptionFor(
      event({
        description: 'Ein Abend mit Aussicht',
        descriptionLanguage: 'de',
        descriptionAlt: 'An evening with a view',
        descriptionAltLanguage: 'en',
        descriptionAltOrigin: 'PUBLISHER',
      }),
      'en',
    )

    expect(chosen).toEqual({ text: 'An evening with a view', lang: 'en', machine: false })
  })

  // The disclosure the page shows hangs off this flag, and ADR-026 makes it non-optional.
  it('reports a machine translation as one', () => {
    const chosen = descriptionFor(
      event({
        description: 'Ein Abend mit Aussicht',
        descriptionLanguage: 'de',
        descriptionAlt: 'An evening with a view',
        descriptionAltLanguage: 'en',
        descriptionAltOrigin: 'MACHINE',
      }),
      'en',
    )

    expect(chosen).toEqual({ text: 'An evening with a view', lang: 'en', machine: true })
  })

  // A description a visitor cannot read still says who is playing. It stays, marked.
  it('falls back to the original in its own language', () => {
    const chosen = descriptionFor(
      event({ description: 'Ein Abend mit Aussicht', descriptionLanguage: 'de' }),
      'en',
    )

    expect(chosen).toEqual({ text: 'Ein Abend mit Aussicht', lang: 'de', machine: false })
  })

  // An unclassified text claims nothing rather than claiming the page's locale.
  it('declares no language for an unclassified description', () => {
    const chosen = descriptionFor(event({ description: 'Doors 19:30' }), 'en')

    expect(chosen).toEqual({ text: 'Doors 19:30', lang: null, machine: false })
  })

  it('returns null when the event has no description', () => {
    expect(descriptionFor(event(), 'en')).toBeNull()
    expect(descriptionFor(event({ descriptionWithheld: true }), 'de')).toBeNull()
  })

  // A language outside the two the site publishes is data we cannot act on, so it is not a `lang`.
  it('ignores a language it does not publish', () => {
    const chosen = descriptionFor(
      event({ description: 'Un dúo argentino', descriptionLanguage: 'es' }),
      'en',
    )

    expect(chosen).toEqual({ text: 'Un dúo argentino', lang: null, machine: false })
  })
})
