import { describe, expect, it } from 'vitest'

import { imageCredit } from '@/lib/imageCredit'

/**
 * Whether an image may be shown, and what has to be shown with it.
 *
 * A CC BY image rendered without its credit is a licence breach rather than a cosmetic gap, so a
 * half-filled row must produce nothing at all. The API refuses to store one; this is the second
 * line, against a row that predates the rule or arrives from a future source.
 */
describe('imageCredit', () => {
  const complete = {
    imageUrl: 'https://upload.wikimedia.org/example.jpg',
    imageAttribution: 'Photographer Name, via Wikimedia Commons',
    imageLicenceId: 'CC-BY-SA-4.0',
    imageSourceUrl: 'https://commons.wikimedia.org/wiki/File:Example.jpg',
  }

  it('credits the author and links the licence deed', () => {
    expect(imageCredit(complete)).toEqual({
      attribution: 'Photographer Name, via Wikimedia Commons',
      sourceUrl: 'https://commons.wikimedia.org/wiki/File:Example.jpg',
      licenceLabel: 'CC BY-SA 4.0',
      licenceUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
    })
  })

  it('gives a public-domain image a label and no deed to link', () => {
    const credit = imageCredit({ ...complete, imageLicenceId: 'PD' })

    expect(credit?.licenceLabel).toBe('Public domain')
    expect(credit?.licenceUrl).toBeNull()
  })

  it('shows an unknown identifier as it stands rather than guessing a deed', () => {
    const credit = imageCredit({ ...complete, imageLicenceId: 'CC-BY-NC-4.0' })

    expect(credit?.licenceLabel).toBe('CC-BY-NC-4.0')
    expect(credit?.licenceUrl).toBeNull()
  })

  it.each(['imageAttribution', 'imageLicenceId', 'imageSourceUrl'] as const)(
    'renders nothing when %s is missing',
    (field) => {
      expect(imageCredit({ ...complete, [field]: null })).toBeNull()
    },
  )

  it('renders nothing for a row with no image', () => {
    expect(imageCredit({ ...complete, imageUrl: null })).toBeNull()
    expect(imageCredit(null)).toBeNull()
  })
})
