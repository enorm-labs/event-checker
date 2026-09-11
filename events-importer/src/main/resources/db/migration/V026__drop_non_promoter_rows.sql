-- Deletes the promoter rows a review found to name no promoter (#1318, docs/promoters/REVIEWED.tsv):
-- fragments and titles a scraper put in the promoter slot. `isNonPromoterName` now refuses each of
-- them on import; this removes the rows that exist, because there is no re-seed on staging or
-- production. Keyed on slug and a no-op where a row is absent; the FK cascade on
-- event_promoter.promoter_id removes the links, and the events stay.
--
-- Huxleys' slug-minted rows (`jb-freie`, `jm-audio`, `we-artists`) are deliberately not here: they
-- are real promoters with a casing defect, and #307 is where that is fixed.
--
-- Unqualified table name, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).

DELETE FROM promoter
WHERE slug IN (
    'act', 'ar', 'bum', 'channel', 'das-forgotten-female-composers', 'itd',
    'kneipenabend', 'kuratorin-anika-meier', 'leasing-rent', 'mfp', 'mup', 'niemals', 'nova',
    'peter-edel-www-stummfilmkonzerte-de', 'qu', 'slam', 'spirit', 'sunday-matinee',
    'tag-der-klubkultur', 'team-feel-free-energy', 'the-hilarious-deep-amazing-comedy'
);
