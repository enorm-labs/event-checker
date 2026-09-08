-- The language of each description, and room for the same text in the other locale (ADR-026, #470).
--
-- Columns rather than a per-language table. The site has two locales, so an event holds one
-- original and at most one other text. One row keeps the detail read a single query, and the
-- prohibition gate (#807) clears everything with the one UPDATE it already runs. A third locale is
-- the moment to move to rows, and these columns migrate cleanly.
--
-- Unqualified table name, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).

-- `description_language` is detected at import. NULL means unknown, which is an honest answer for
-- a two-line text or a field that holds both languages, not a missing detection. No backfill here:
-- detection runs in the importer, and the admin API replays it over the stored rows once.
ALTER TABLE event
    ADD COLUMN description_language            TEXT,
    ADD COLUMN description_language_confidence NUMERIC(4, 3),
    ADD COLUMN description_alt                 TEXT,
    ADD COLUMN description_alt_language        TEXT,
    ADD COLUMN description_alt_origin          TEXT,
    ADD COLUMN description_alt_engine          TEXT,
    ADD COLUMN description_alt_source_hash     TEXT;

-- The application writes these through one code path, and the constraints are the second line
-- against a hand-edited row. The alt text is all-or-nothing: a text with no language or origin
-- cannot be served, and a language with no text is a stale marker.
ALTER TABLE event
    ADD CONSTRAINT event_description_language_valid
        CHECK (description_language IS NULL OR description_language IN ('de', 'en')),
    ADD CONSTRAINT event_description_alt_language_valid
        CHECK (description_alt_language IS NULL OR description_alt_language IN ('de', 'en')),
    ADD CONSTRAINT event_description_alt_origin_valid
        CHECK (description_alt_origin IS NULL OR description_alt_origin IN ('PUBLISHER', 'MACHINE')),
    ADD CONSTRAINT event_description_alt_complete
        CHECK (
            (description_alt IS NULL AND description_alt_language IS NULL AND description_alt_origin IS NULL)
            OR (description_alt IS NOT NULL AND description_alt_language IS NOT NULL AND description_alt_origin IS NOT NULL)
        );
