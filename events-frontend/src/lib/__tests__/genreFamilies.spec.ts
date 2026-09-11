import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

import { LOCALES } from '@/i18n/locales'
import { GENRE_FAMILIES } from '@/lib/genreFamilies'

/**
 * The family select renders `events.filters.families.<slug>` for every slug in the constant, and
 * a missing key shows the slug instead of failing — so this is the check that a family added on
 * the backend, or renamed here, has a label in every language. Read from disk for the reason
 * `messages.spec.ts` gives.
 */
describe('GENRE_FAMILIES', () => {
  it.each(LOCALES)('has a %s label for every family', (locale) => {
    const file = resolve(process.cwd(), `src/i18n/messages/${locale}/events.json`)
    const families = JSON.parse(readFileSync(file, 'utf8')).filters.families as Record<
      string,
      string
    >

    expect(Object.keys(families).sort()).toEqual([...GENRE_FAMILIES].sort())
    for (const slug of GENRE_FAMILIES) expect(families[slug]).toBeTruthy()
  })

  it('lists each family once', () => {
    expect(new Set(GENRE_FAMILIES).size).toBe(GENRE_FAMILIES.length)
  })
})
