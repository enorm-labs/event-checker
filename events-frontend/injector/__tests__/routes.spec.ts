import { describe, expect, it } from 'vitest'

import { matchDetailRoute } from '../routes.ts'

/**
 * The route matcher is the injector's whole surface: what it accepts reaches the BFF as a URL, so
 * the refusals matter more than the matches.
 */
describe('matchDetailRoute', () => {
  it('recognises each of the four families in either locale', () => {
    expect(matchDetailRoute('/en/events/2026-06-12-lido-test-act')).toEqual({
      kind: 'events',
      locale: 'en',
      slug: '2026-06-12-lido-test-act',
      path: '/events/2026-06-12-lido-test-act',
    })
    expect(matchDetailRoute('/de/venues/lido')?.kind).toBe('venues')
    expect(matchDetailRoute('/de/artists/test-act')?.locale).toBe('de')
    expect(matchDetailRoute('/en/promoters/somebody')?.path).toBe('/promoters/somebody')
  })

  it('ignores the query string and the fragment, as the client canonical does', () => {
    expect(matchDetailRoute('/en/venues/lido?from=share#tickets')?.slug).toBe('lido')
  })

  it('tolerates a trailing slash', () => {
    expect(matchDetailRoute('/en/venues/lido/')?.slug).toBe('lido')
  })

  it('refuses everything that is not a detail page', () => {
    const paths = ['/', '/en', '/en/events', '/en/calendar', '/assets/index-abc.js', '/en/legal/imprint']
    // Mapped rather than looped, so a failure names the path that matched.
    expect(paths.map((path) => [path, matchDetailRoute(path)])).toEqual(paths.map((path) => [path, null]))
  })

  it('refuses a slug the BFF could never have minted, before it becomes a URL', () => {
    const paths = [
      '/en/events/../admin',
      '/en/events/%2e%2e',
      '/en/events/Test-Act',
      '/en/events/a b',
      '/en/events/-leading',
      '/fr/events/test-act',
      '/en/things/test-act',
      '/en/events/test-act/extra',
    ]
    expect(paths.map((path) => [path, matchDetailRoute(path)])).toEqual(paths.map((path) => [path, null]))
  })
})
