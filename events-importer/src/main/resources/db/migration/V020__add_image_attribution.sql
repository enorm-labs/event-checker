-- Who took each venue, artist and promoter image, and under what licence (#1275).
--
-- These three `image_url` columns are entered by hand through the admin API. Nothing scrapes them
-- and no migration writes them, so SCRAPING_POSITION.md §3 excuses them from the per-source licence
-- question that governs `event.image_url`. That excuse holds only while a maintainer picks each URL
-- personally. It does not survive filling the columns from an archive, which is what #1275 does, and
-- a CC BY or CC BY-SA file is unusable without the credit beside it.
--
-- `image_licence_id` is deliberately not called `image_licence`. `event_source.image_licence` is a
-- closed vocabulary we authored -- PERMITTED, PROHIBITED, UNCLEAR -- and answers whether we may show
-- a source's material at all. This column answers which licence a third party published a file
-- under, and its values are SPDX identifiers: CC0-1.0, CC-BY-4.0, CC-BY-SA-4.0. Two columns with one
-- name and two vocabularies is a mistake a reader makes once, in the direction of showing something
-- they should not.
--
-- No CHECK on the value, unlike V006. That vocabulary is ours and closed; this one is Creative
-- Commons' and open, so a new licence version would be a deployment rather than a row.
--
-- Unqualified table names, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).

ALTER TABLE venue
    ADD COLUMN image_attribution TEXT,
    ADD COLUMN image_licence_id  TEXT,
    ADD COLUMN image_source_url  TEXT;

ALTER TABLE artist
    ADD COLUMN image_attribution TEXT,
    ADD COLUMN image_licence_id  TEXT,
    ADD COLUMN image_source_url  TEXT;

ALTER TABLE promoter
    ADD COLUMN image_attribution TEXT,
    ADD COLUMN image_licence_id  TEXT,
    ADD COLUMN image_source_url  TEXT;

-- An image with no credit cannot be displayed, so the row is invalid rather than partial. The
-- request DTOs reject the same combination; this is the second line V006 describes, against a row
-- edited by hand.
--
-- Validating rather than NOT VALID, and that is the decision worth recording. NOT VALID would let a
-- pre-existing uncredited row survive and be served uncredited, which is the defect. All three
-- columns held 0 non-null `image_url` on staging when this was written, so the constraint is
-- expected to validate against nothing.
ALTER TABLE venue
    ADD CONSTRAINT venue_image_attributed
        CHECK (image_url IS NULL OR (image_attribution IS NOT NULL AND image_licence_id IS NOT NULL AND image_source_url IS NOT NULL));

ALTER TABLE artist
    ADD CONSTRAINT artist_image_attributed
        CHECK (image_url IS NULL OR (image_attribution IS NOT NULL AND image_licence_id IS NOT NULL AND image_source_url IS NOT NULL));

ALTER TABLE promoter
    ADD CONSTRAINT promoter_image_attributed
        CHECK (image_url IS NULL OR (image_attribution IS NOT NULL AND image_licence_id IS NOT NULL AND image_source_url IS NOT NULL));
