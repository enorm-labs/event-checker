// Berlin's 12 boroughs (Bezirke). Venues carry the canonical slug; the label is what users see.
// Kept as a static list since the set is fixed. Shared by the events filter and the venue list.

export interface District {
  slug: string
  label: string
}

export const DISTRICTS: readonly District[] = [
  { slug: 'mitte', label: 'Mitte' },
  { slug: 'friedrichshain-kreuzberg', label: 'Friedrichshain-Kreuzberg' },
  { slug: 'pankow', label: 'Pankow' },
  { slug: 'charlottenburg-wilmersdorf', label: 'Charlottenburg-Wilmersdorf' },
  { slug: 'spandau', label: 'Spandau' },
  { slug: 'steglitz-zehlendorf', label: 'Steglitz-Zehlendorf' },
  { slug: 'tempelhof-schoeneberg', label: 'Tempelhof-Schöneberg' },
  { slug: 'neukoelln', label: 'Neukölln' },
  { slug: 'treptow-koepenick', label: 'Treptow-Köpenick' },
  { slug: 'marzahn-hellersdorf', label: 'Marzahn-Hellersdorf' },
  { slug: 'lichtenberg', label: 'Lichtenberg' },
  { slug: 'reinickendorf', label: 'Reinickendorf' },
]

const DISTRICT_LABELS: Record<string, string> = Object.fromEntries(
  DISTRICTS.map((d) => [d.slug, d.label]),
)

/** Maps a district slug to its display label, falling back to the raw slug if unknown. */
export function districtLabel(slug?: string | null): string {
  if (!slug) return ''
  return DISTRICT_LABELS[slug] ?? slug
}
