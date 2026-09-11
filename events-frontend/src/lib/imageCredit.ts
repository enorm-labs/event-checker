/**
 * The credit a venue, artist or promoter image has to carry (#1275).
 *
 * These images come from Wikimedia Commons and comparable archives, where CC BY and CC BY-SA both
 * require the author, the licence and a link to the original beside the picture. The API returns
 * the three fields together, so a partial credit means a row that should not have been accepted.
 *
 * Resizing is a format change under CC 4.0 § 2(a)(4) and does not produce Adapted Material, so a
 * derivative carries the original's credit unchanged and share-alike never reaches the site.
 */

/** A row that may carry an attributed image. Every field is optional — the BFF marks none required. */
export interface AttributedImage {
  imageUrl?: string | null
  imageAttribution?: string | null
  imageLicenceId?: string | null
  imageSourceUrl?: string | null
}

export interface ImageCredit {
  attribution: string
  sourceUrl: string
  licenceLabel: string
  /** The licence deed, or `null` for a public-domain image, which has no deed to link. */
  licenceUrl: string | null
}

/**
 * Deed URL and display label per SPDX identifier.
 *
 * Listed rather than derived from the identifier. A pattern over `CC-BY-…` would also produce a URL
 * for an identifier Creative Commons does not publish, and a wrong licence link is worse than none.
 * It is the same reason the `-DE` ports are spelled out: they are separate licences with their own
 * deeds, not a suffix on the international ones.
 *
 * **Every identifier `SPDX` in `scripts/venue-images.py` can write appears here.** A file
 * whose licence resolves there but not here still credits its author and shows the identifier, with
 * no deed to click -- correct, and it reads as a raw code rather than a licence.
 */
const LICENCES: Record<string, { label: string; url: string | null }> = {
  'CC0-1.0': { label: 'CC0 1.0', url: 'https://creativecommons.org/publicdomain/zero/1.0/' },
  'CC-BY-2.0': { label: 'CC BY 2.0', url: 'https://creativecommons.org/licenses/by/2.0/' },
  'CC-BY-2.5': { label: 'CC BY 2.5', url: 'https://creativecommons.org/licenses/by/2.5/' },
  'CC-BY-3.0': { label: 'CC BY 3.0', url: 'https://creativecommons.org/licenses/by/3.0/' },
  'CC-BY-3.0-DE': { label: 'CC BY 3.0 DE', url: 'https://creativecommons.org/licenses/by/3.0/de/' },
  'CC-BY-4.0': { label: 'CC BY 4.0', url: 'https://creativecommons.org/licenses/by/4.0/' },
  'CC-BY-SA-2.0': { label: 'CC BY-SA 2.0', url: 'https://creativecommons.org/licenses/by-sa/2.0/' },
  'CC-BY-SA-2.0-DE': {
    label: 'CC BY-SA 2.0 DE',
    url: 'https://creativecommons.org/licenses/by-sa/2.0/de/',
  },
  'CC-BY-SA-2.5': { label: 'CC BY-SA 2.5', url: 'https://creativecommons.org/licenses/by-sa/2.5/' },
  'CC-BY-SA-3.0': { label: 'CC BY-SA 3.0', url: 'https://creativecommons.org/licenses/by-sa/3.0/' },
  'CC-BY-SA-3.0-DE': {
    label: 'CC BY-SA 3.0 DE',
    url: 'https://creativecommons.org/licenses/by-sa/3.0/de/',
  },
  'CC-BY-SA-4.0': { label: 'CC BY-SA 4.0', url: 'https://creativecommons.org/licenses/by-sa/4.0/' },
  PD: { label: 'Public domain', url: null },
}

/**
 * The credit for [row], or `null` where there is no image or no complete credit.
 *
 * An incomplete credit returns `null` rather than a partial line, because the caller then renders
 * nothing and the missing half is visible as an absent caption rather than as a dangling label.
 * An unknown identifier still credits the author and shows the identifier as it stands.
 */
export function imageCredit(row: AttributedImage | null | undefined): ImageCredit | null {
  if (!row?.imageUrl || !row.imageAttribution || !row.imageLicenceId || !row.imageSourceUrl) {
    return null
  }
  const licence = LICENCES[row.imageLicenceId]
  return {
    attribution: row.imageAttribution,
    sourceUrl: row.imageSourceUrl,
    licenceLabel: licence?.label ?? row.imageLicenceId,
    licenceUrl: licence?.url ?? null,
  }
}
