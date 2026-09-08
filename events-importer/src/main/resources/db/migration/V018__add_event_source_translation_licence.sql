-- Whether a source grants us the right to translate its descriptions (ADR-026, #470).
--
-- A third column beside `description_licence` and `image_licence`, for the reason V006 gives about
-- the first two: the answers differ. A venue may allow us to show its text and still object to a
-- machine translation of it, and § 23 UrhG makes a translation a separate act from the display.
-- Parsing that answer out of `licence_note` would put a legal condition behind a substring search.
--
-- No default and no backfill, so an unreviewed source reads as unreviewed. Null is not `UNCLEAR`
-- and neither of them permits: translation is the one field whose rule is fail-closed, because only
-- `PERMITTED` allows the act at all.
--
-- Unqualified table name, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).
ALTER TABLE event_source
    ADD COLUMN translation_licence TEXT;

-- The admin API validates against the SourceLicence enum. This is the second line, for a row edited
-- by hand.
ALTER TABLE event_source
    ADD CONSTRAINT event_source_translation_licence_valid
        CHECK (translation_licence IS NULL OR translation_licence IN ('PERMITTED', 'PROHIBITED', 'UNCLEAR'));
