-- A description per promoter, in up to two languages (#328).
--
-- The same four columns `venue` gained in V019, for the same reasons: two locales, one row per
-- promoter, one query for the detail page. Both texts are our own prose, written by hand through
-- the admin API, so there is no origin column and nothing to disclose. A promoter's own site is not
-- scraped for this — no `event_source` row records a promoter's terms, so the licence gate that
-- covers a scraped event description cannot reach a promoter (SCRAPING_POSITION.md §3.6).
--
-- Unqualified table name, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).
ALTER TABLE promoter
    ADD COLUMN description              TEXT,
    ADD COLUMN description_language     TEXT,
    ADD COLUMN description_alt          TEXT,
    ADD COLUMN description_alt_language TEXT;

-- The same shape as `venue`: a language is one of the two locales, and the alt text is
-- all-or-nothing, because a text with no language cannot be served and a language with no text is a
-- stale marker.
ALTER TABLE promoter
    ADD CONSTRAINT promoter_description_language_valid
        CHECK (description_language IS NULL OR description_language IN ('de', 'en')),
    ADD CONSTRAINT promoter_description_alt_language_valid
        CHECK (description_alt_language IS NULL OR description_alt_language IN ('de', 'en')),
    ADD CONSTRAINT promoter_description_alt_complete
        CHECK (
            (description_alt IS NULL AND description_alt_language IS NULL)
            OR (description_alt IS NOT NULL AND description_alt_language IS NOT NULL)
        );
