-- Moves every venue from its borough to its pre-2001 district, and closes the column to those 23 (#1307).
--
-- V009 closed `district` to Berlin's twelve boroughs. That fixed the mix of levels that started #329,
-- and left the filter unable to say "Kreuzberg": 36 of the 86 venues sat in `friedrichshain-kreuzberg`
-- and 12 in `pankow`, which is Prenzlauer Berg for all but two of them. The 23 districts of 1986-2000
-- are the names Berliners use, and each is a union of today's Ortsteile, so the reassignment below is
-- a lookup and not a judgement.
--
-- Every slug is a venue the seed file creates, and MigrationSlugTest asserts it. Each UPDATE is
-- guarded on the borough the row holds today, like V009, so a row somebody already moved is left
-- alone. Where OpenStreetMap's Ortsteil and the postal code disagreed -- Huxleys Neue Welt on the
-- Hasenheide, Heideglühen at the Westhafen, MAXXIM on the Joachimsthaler Straße -- the assignment was
-- checked by hand and is recorded in the pull request, not derived from the geocoder.
--
-- Sixteen venues keep their value: `mitte`, `lichtenberg`, `neukoelln` and `spandau` are the same
-- word at both levels, and the venues holding them are in the old district of that name, not in
-- Tiergarten, Wedding or Hohenschönhausen. The borough `mitte` also covered Wedding, which is why the
-- five Wedding venues are moved off it below.
--
-- Unqualified table name, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).

-- The constraint first, because every value written below is one V009 forbids.
ALTER TABLE venue DROP CONSTRAINT venue_district_valid;

-- Friedrichshain: 20 of the friedrichshain-kreuzberg venues.
UPDATE venue SET district = 'friedrichshain'
WHERE slug IN (
    'astra-kulturhaus', 'badehaus', 'berghain-panorama-bar', 'cassiopeia',
    'club-ost', 'crack-bellmer', 'der-weisse-hase', 'kantine-am-berghain',
    'kater', 'loge', 'maaya', 'matrix',
    'monster-ronson-s-ichiban-karaoke', 'neue-zukunft', 'renate', 'saalchen',
    'supamolly', 'uber-arena', 'uber-eats-music-hall', 'urban-spree'
)
  AND district = 'friedrichshain-kreuzberg';

-- Kreuzberg: 16 of the friedrichshain-kreuzberg venues.
UPDATE venue SET district = 'kreuzberg'
WHERE slug IN (
    'aeden', 'arcanoa', 'bi-nuu', 'clash',
    'gretchen', 'junction-bar', 'lido', 'madame-claude',
    'modus-berlin', 'monarch', 'morphine-raum', 'privatclub',
    'ritter-butzke', 'so36', 'tempodrom', 'wild-at-heart'
)
  AND district = 'friedrichshain-kreuzberg';

-- Prenzlauer Berg: 10 of the pankow venues.
UPDATE venue SET district = 'prenzlauer-berg'
WHERE slug IN (
    'alte-kantine', 'colosseum', 'cosmic-comedy-club', 'duncker-club',
    'frannz-club', 'max-schmeling-halle', 'roadrunner-s-paradise', 'soda-club',
    'ufo-im-velodrom', 'velodrom'
)
  AND district = 'pankow';

-- Weißensee: 2 of the pankow venues.
UPDATE venue SET district = 'weissensee'
WHERE slug IN (
    'kulturhaus-peter-edel', 'theater-im-delphi'
)
  AND district = 'pankow';

-- Wedding: 5 of the mitte venues.
UPDATE venue SET district = 'wedding'
WHERE slug IN (
    'heidegluhen', 'humboldthain-club', 'migas', 'panke-culture',
    'silent-green'
)
  AND district = 'mitte';

-- Treptow: 6 of the treptow-koepenick venues.
UPDATE venue SET district = 'treptow'
WHERE slug IN (
    'club-der-visionare', 'festsaal-kreuzberg', 'kulturhaus-insel-berlin', 'ms-hoppetosse',
    'sonnenraum', 'zenner'
)
  AND district = 'treptow-koepenick';

-- Köpenick: 1 of the treptow-koepenick venues.
UPDATE venue SET district = 'koepenick'
WHERE slug IN (
    'parkbuhne-wuhlheide'
)
  AND district = 'treptow-koepenick';

-- Schöneberg: 4 of the tempelhof-schoeneberg venues.
UPDATE venue SET district = 'schoeneberg'
WHERE slug IN (
    'havanna', 'metropol', 'mikropol', 'urania'
)
  AND district = 'tempelhof-schoeneberg';

-- Tempelhof: 2 of the tempelhof-schoeneberg venues.
UPDATE venue SET district = 'tempelhof'
WHERE slug IN (
    'columbia-theater', 'columbiahalle'
)
  AND district = 'tempelhof-schoeneberg';

-- Charlottenburg: 2 of the charlottenburg-wilmersdorf venues.
UPDATE venue SET district = 'charlottenburg'
WHERE slug IN (
    'maxxim', 'quasimodo'
)
  AND district = 'charlottenburg-wilmersdorf';

-- Wilmersdorf: 1 of the charlottenburg-wilmersdorf venues.
UPDATE venue SET district = 'wilmersdorf'
WHERE slug IN (
    'bar-jeder-vernunft'
)
  AND district = 'charlottenburg-wilmersdorf';

-- Marzahn: 1 of the marzahn-hellersdorf venues.
UPDATE venue SET district = 'marzahn'
WHERE slug IN (
    'garten-der-welt'
)
  AND district = 'marzahn-hellersdorf';

-- All 23 rather than the 16 in use, for the reason V009 gave: a venue in Steglitz, Reinickendorf or
-- Hohenschönhausen is a venue we have not added yet, not an invalid value. NULL still means nobody
-- recorded one.
ALTER TABLE venue
    ADD CONSTRAINT venue_district_valid
        CHECK (district IS NULL OR district IN (
            'charlottenburg',
            'friedrichshain',
            'hellersdorf',
            'hohenschoenhausen',
            'koepenick',
            'kreuzberg',
            'lichtenberg',
            'marzahn',
            'mitte',
            'neukoelln',
            'pankow',
            'prenzlauer-berg',
            'reinickendorf',
            'schoeneberg',
            'spandau',
            'steglitz',
            'tempelhof',
            'tiergarten',
            'treptow',
            'wedding',
            'weissensee',
            'wilmersdorf',
            'zehlendorf'
        ));
