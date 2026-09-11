// The thirteen genre families the filter offers, in display order — the same order and slugs as
// `GenreFamily` in events-core. Labels live in i18n under `events.filters.families.<slug>`, so a
// family the frontend does not know renders its slug, which is the drift signal (#363).

export const GENRE_FAMILIES: readonly string[] = [
  'electronic',
  'hip-hop',
  'pop',
  'rock',
  'punk',
  'metal',
  'wave',
  'soul-funk',
  'jazz-blues',
  'folk',
  'latin-world',
  'classical',
  'charts',
]
