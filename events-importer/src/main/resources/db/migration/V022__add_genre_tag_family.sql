-- One family per genre tag, so the filter can offer thirteen choices instead of 173 (#363).
--
-- The value is a slug from the closed GenreFamily vocabulary in events-core: `electronic`,
-- `hip-hop`, `rock` and so on. No CHECK on it, because the vocabulary is code and a new family
-- would then need a migration beside the enum; GenreFamilyReconciler rewrites every row from the
-- code on each importer start, which is also why no backfill happens here. NULL means the code
-- names no family for the tag, and the frontend then shows the tag in neither select.
--
-- Unqualified table name, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).

ALTER TABLE genre_tag
    ADD COLUMN family TEXT;

-- The public event search filters on it with a correlated EXISTS per event.
CREATE INDEX idx_genre_tag_family ON genre_tag (family);
