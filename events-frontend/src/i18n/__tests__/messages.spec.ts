import { describe, expect, it } from 'vitest'

import en from '@/i18n/messages/en'
import { DEFAULT_LOCALE, LOCALES } from '@/i18n/locales'

/**
 * Guards the message catalogues against the two failures that ship silently.
 *
 * A missing key falls back to English rather than erroring, so a half-translated locale looks
 * fine in review and fine in the browser — until a German reader hits an English sentence. And an
 * *extra* key is a translation of something that no longer exists, which is how a catalogue quietly
 * grows dead weight.
 *
 * With only English published this compares English against itself, which is not yet interesting.
 * It is here now so that the moment `de` joins `LOCALES` (Phase 3), parity is already enforced
 * rather than being remembered.
 */

/** All leaf key paths, e.g. `common.nav.events`. */
function keyPaths(messages: object, prefix = ''): string[] {
  return Object.entries(messages).flatMap(([key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key
    return typeof value === 'object' && value !== null ? keyPaths(value, path) : [path]
  })
}

const catalogues: Record<string, object> = { en }

describe('message catalogues', () => {
  it('has a catalogue for every published locale', () => {
    // The one that actually bites: adding a locale to LOCALES makes its URLs routable
    // immediately, so a catalogue that does not exist yet renders as English under a foreign URL.
    for (const locale of LOCALES) {
      expect(catalogues[locale], `no catalogue for published locale "${locale}"`).toBeDefined()
    }
  })

  it('gives every locale the same keys as the fallback', () => {
    const reference = keyPaths(catalogues[DEFAULT_LOCALE]!).sort()

    for (const [locale, messages] of Object.entries(catalogues)) {
      const actual = keyPaths(messages).sort()
      const missing = reference.filter((key) => !actual.includes(key))
      const extra = actual.filter((key) => !reference.includes(key))

      expect(missing, `"${locale}" is missing keys`).toEqual([])
      expect(extra, `"${locale}" has keys the fallback does not`).toEqual([])
    }
  })

  it('has no empty strings, which render as a blank space rather than an error', () => {
    for (const [locale, messages] of Object.entries(catalogues)) {
      const empties = keyPaths(messages).filter((path) => {
        const value = path
          .split('.')
          .reduce<unknown>((node, key) => (node as Record<string, unknown>)?.[key], messages)
        return typeof value === 'string' && value.trim() === ''
      })
      expect(empties, `"${locale}" has empty messages`).toEqual([])
    }
  })

  it('keeps named interpolations consistent across locales', () => {
    // `{subject}` in one language and `{thing}` in another renders the literal placeholder to the
    // user. Compare the placeholder sets rather than the prose.
    const placeholders = (messages: object, path: string) => {
      const value = path
        .split('.')
        .reduce<unknown>((node, key) => (node as Record<string, unknown>)?.[key], messages)
      return typeof value === 'string'
        ? [...value.matchAll(/\{(\w+)\}/g)].map((m) => m[1]).sort()
        : []
    }

    for (const [locale, messages] of Object.entries(catalogues)) {
      for (const path of keyPaths(catalogues[DEFAULT_LOCALE]!)) {
        expect(placeholders(messages, path), `"${locale}" at "${path}"`).toEqual(
          placeholders(catalogues[DEFAULT_LOCALE]!, path),
        )
      }
    }
  })
})
