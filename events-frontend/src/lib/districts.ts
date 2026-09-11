// Berlin's 23 pre-2001 districts (Bezirke as they stood from 1986 to 2000), not the 12 boroughs that
// replaced them: Kreuzberg and Friedrichshain, not Friedrichshain-Kreuzberg (#1307). Venues carry the
// canonical slug; the label is what users see. Static because the set is closed, and alphabetical
// because a dropdown of 23 is read, not scanned. Shared by the events filter and the venue list.

export interface District {
  slug: string
  label: string
}

export const DISTRICTS: readonly District[] = [
  { slug: 'charlottenburg', label: 'Charlottenburg' },
  { slug: 'friedrichshain', label: 'Friedrichshain' },
  { slug: 'hellersdorf', label: 'Hellersdorf' },
  { slug: 'hohenschoenhausen', label: 'Hohenschönhausen' },
  { slug: 'koepenick', label: 'Köpenick' },
  { slug: 'kreuzberg', label: 'Kreuzberg' },
  { slug: 'lichtenberg', label: 'Lichtenberg' },
  { slug: 'marzahn', label: 'Marzahn' },
  { slug: 'mitte', label: 'Mitte' },
  { slug: 'neukoelln', label: 'Neukölln' },
  { slug: 'pankow', label: 'Pankow' },
  { slug: 'prenzlauer-berg', label: 'Prenzlauer Berg' },
  { slug: 'reinickendorf', label: 'Reinickendorf' },
  { slug: 'schoeneberg', label: 'Schöneberg' },
  { slug: 'spandau', label: 'Spandau' },
  { slug: 'steglitz', label: 'Steglitz' },
  { slug: 'tempelhof', label: 'Tempelhof' },
  { slug: 'tiergarten', label: 'Tiergarten' },
  { slug: 'treptow', label: 'Treptow' },
  { slug: 'wedding', label: 'Wedding' },
  { slug: 'weissensee', label: 'Weißensee' },
  { slug: 'wilmersdorf', label: 'Wilmersdorf' },
  { slug: 'zehlendorf', label: 'Zehlendorf' },
]

const DISTRICT_LABELS: Record<string, string> = Object.fromEntries(
  DISTRICTS.map((d) => [d.slug, d.label]),
)

/** Maps a district slug to its display label, falling back to the raw slug if unknown. */
export function districtLabel(slug?: string | null): string {
  if (!slug) return ''
  return DISTRICT_LABELS[slug] ?? slug
}
