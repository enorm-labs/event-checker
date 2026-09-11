-- Merges the promoter rows that were one promoter under two spellings, and gives the survivors the
-- display name the normalizer now produces (#328, #304).
--
-- The normalizer stops these pairs recurring, and only reaches a row the next import creates: a
-- promoter row keeps the name and slug of whichever import minted it, and a past event is never
-- scraped again. So the rows that exist are fixed here, and the pairs are the ones a person confirmed
-- against staging, out of the groups `scripts/promoter-duplicates.py` reports.
--
-- Four steps, each keyed on slug and each a no-op where the row is absent, so a database without
-- these rows (every developer's, and any environment that never scraped them) is left alone:
--   1. a survivor that does not exist yet is made out of one of its losers, by renaming it;
--   2. every survivor gets its display name;
--   3. the losers' event links move to the survivor, skipping an event already linked to it;
--   4. the losers are deleted, and the FK cascade removes the links that were skipped in 3.
-- A pair whose loser is its own survivor is a rename only (`tv-noir` stays `tv-noir`, as "TV Noir").
--
-- A co-billing such as "puschen & little league shows" is one row for two promoters. The scrapers now
-- split them, so the upcoming events re-link on the next import; this moves the row's remaining
-- events to the first-named promoter, which loses the second name on those past events and nothing
-- else.
--
-- Unqualified table names, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).

CREATE TEMPORARY TABLE promoter_merge (
    loser         TEXT NOT NULL,
    survivor      TEXT NOT NULL,
    survivor_name TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO promoter_merge (loser, survivor, survivor_name) VALUES
    -- Legal forms and descriptors the normalizer now strips.
    ('antonio-garcia-einzelunternehmer',              'antonio-garcia',        'Antonio Garcia'),
    ('concert-concept-veranstaltungs',                'concert-concept',       'Concert Concept'),
    ('concert-concept-veranstaltungs-gmbh',           'concert-concept',       'Concert Concept'),
    ('fkp-scorpio-konzertproduktionen',               'fkp-scorpio',           'FKP Scorpio'),
    ('fkp-scorpio',                                   'fkp-scorpio',           'FKP Scorpio'),
    ('landstreicher-kulturproduktionen',              'landstreicher-konzerte', 'Landstreicher Konzerte'),
    ('landstreicher',                                 'landstreicher-konzerte', 'Landstreicher Konzerte'),
    ('boldt-berlin-konzertagentur',                   'boldt-berlin',          'Boldt Berlin'),
    ('kaenguruh-production-konzertagentur',           'kanguruh-production',   'Känguruh Production'),
    ('kanguruh-production-konzertagentur',            'kanguruh-production',   'Känguruh Production'),
    -- Spellings the normalizer now folds.
    ('all-room',                                      'all-rooms',             'All Rooms'),
    ('atok',                                          'atok-berlin',           'ATOK Berlin'),
    ('audiolith-international',                       'audiolith',             'Audiolith'),
    ('streetlife',                                    'streetlife-international', 'Streetlife International'),
    ('listenagency',                                  'friendly-reminder',     'Friendly Reminder'),
    ('greyzone-concerts-promotion-grey-von-bronikowski', 'greyzone',           'Greyzone Concerts'),
    ('greyzone',                                      'greyzone',              'Greyzone Concerts'),
    ('messedup-magazine',                             'messed-up-magazine',    'Messed!Up Magazine'),
    ('musikblog-de',                                  'musikblog',             'MusikBlog'),
    ('musikblog',                                     'musikblog',             'MusikBlog'),
    ('prk-dreamhouse',                                'prk-dreamhaus',         'PRK DreamHaus'),
    ('prk-dreamhaus',                                 'prk-dreamhaus',         'PRK DreamHaus'),
    ('rausgeganger',                                  'rausgegangen',          'Rausgegangen'),
    ('punkfilmfestival-berlin',                       'punkfilmfest-berlin',   'punkfilmfest berlin'),
    ('schoneberg',                                    'konzertburo-schoneberg', 'Konzertbüro Schoneberg'),
    ('trinity',                                       'trinity-music',         'Trinity Music'),
    ('tip',                                           'tipberlin',             'tipBerlin'),
    ('tip-berlin',                                    'tipberlin',             'tipBerlin'),
    ('hb',                                            'hb-music',              'HB Music'),
    -- Acronyms the de-shout used to flatten (#304).
    ('tv-noir',                                       'tv-noir',               'TV Noir'),
    ('bossa-fm',                                      'bossa-fm',              'Bossa FM'),
    ('dj-rebel',                                      'dj-rebel',              'DJ Rebel'),
    ('aeg-presents',                                  'aeg-presents',          'AEG Presents'),
    -- A festival note the Peter Edel scraper used to keep in the credit.
    ('rudelsingen-das-original-aus-munster-hinweis-diese-veranstaltung-ist-teil-des-weissenseer-kultursommers-2026',
                                                      'rudelsingen',           'Rudelsingen'),
    ('rudelsingen-das-original-aus-munster',          'rudelsingen',           'Rudelsingen'),
    -- Co-billings the Schokoladen and Urban Spree scrapers now split.
    ('beav-boloney-little-league-shows',              'beav-boloney',          'beav boloney'),
    ('beav-boloney-wild-wax-little-league-shows',     'beav-boloney',          'beav boloney'),
    ('geisburg-records-little-league-shows',          'geisburg',              'Geisburg'),
    ('le-petit-signal-little-league-shows',           'le-petit-signal',       'le petit signal'),
    ('powerline-agency-brighter-agency-little-league-shows', 'powerline-agency', 'Powerline Agency'),
    ('powerline',                                     'powerline-agency',      'Powerline Agency'),
    ('psychberg-affairs-little-league-shows',         'psychberg-affairs',     'psychberg affairs'),
    ('puschen-little-league-shows',                   'puschen',               'Puschen'),
    ('reverberation-little-league-shows',             'reverberation',         'reverberation'),
    ('swamp-booking-little-league-shows',             'swamp',                 'Swamp'),
    ('punkfilmfest-berlin-booking-crunch-tapes',      'punkfilmfest-berlin',   'punkfilmfest berlin'),
    ('positive-transmitter-crunch-tapes',             'positive-transmitter',  'Positive Transmitter'),
    -- The spelling each promoter uses on its own site (docs/promoters/REVIEWED.tsv).
    ('11freunde',                                     '11freunde',             '11FREUNDE'),
    ('aok-die-gesundheitskasse',                      'aok',                   'AOK'),
    ('atoc-soundlab',                                 'atoc-soundlab',         'ATOC Soundlab'),
    ('auf-die-gute-tour',                             'auf-die-gute-tour',     'Auf die gute Tour'),
    ('aufnahme-wiedergabe',                           'aufnahme-wiedergabe',   'aufnahme + wiedergabe'),
    ('berlinkonzerte',                                'new-berlin-konzerte',   'New Berlin Konzerte'),
    ('new-berlin',                                    'new-berlin-konzerte',   'New Berlin Konzerte'),
    ('boese',                                         'boese-live',            'Boese Live'),
    ('bricks',                                        'bricks',                'BRICKS'),
    ('chnsw',                                         'chnsw',                 'CHNSW!'),
    ('diffus',                                        'diffus',                'DIFFUS'),
    ('dlf',                                           'deutschlandfunk',       'Deutschlandfunk'),
    ('doomstar',                                      'doomstar-bookings',     'Doomstar Bookings'),
    ('gotobeat',                                      'gotobeat',              'Gotobeat'),
    ('headline',                                      'headline-concerts',     'Headline Concerts'),
    ('ibb',                                           'ibb-booking',           'IBB Booking'),
    ('kingstar',                                      'kingstar-music',        'Kingstar Music'),
    ('kinky-hub',                                     'kinkyhub-berlin',       'KinkyHub Berlin'),
    ('kulturalarm',                                   'kulturalarm',           'kulturALARM'),
    ('kulturnews',                                    'kulturnews',            'kulturnews'),
    ('lars-berndt',                                   'lars-berndt-events',    'Lars Berndt Events'),
    ('manfred-hertlein-veranstaltungs',               'manfred-hertlein',      'Manfred Hertlein'),
    ('mawi',                                          'mawi-concert',          'MAWI Concert'),
    ('mb-konzerte',                                   'mb-konzerte',           'MB Konzerte'),
    ('mct',                                           'mct-agentur',           'MCT Agentur'),
    ('metal-de',                                      'metal-de',              'metal.de'),
    ('ox-fancine',                                    'ox-fanzine',            'Ox-Fanzine'),
    ('radio-bob',                                     'radio-bob',             'RADIO BOB!'),
    ('radio-france-international',                    'radio-france-internationale', 'Radio France Internationale'),
    ('rockitsessions',                                'rockitsessions',        'Rockitsessions'),
    ('semmel',                                        'semmel-concerts',       'Semmel Concerts'),
    ('stiftung-wissensart',                           'stiftung-wissensart',   'Stiftung Wissensart'),
    ('touringtunes-sp-z-o-o',                         'touringtunes',          'TouringTunes'),
    ('unreleased',                                    'unreleased-berlin',     'Unreleased Berlin'),
    ('zart',                                          'z-art-agency',          'Z|ART Agency');

-- A survivor that is itself a loser would be renamed away before its losers reach it, and the
-- losers would then be neither moved nor deleted. Every loser names the final survivor, and this
-- fails the migration (and the test that runs it) if one day one does not.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM promoter_merge chained
        JOIN promoter_merge onward ON onward.loser = chained.survivor AND onward.loser <> onward.survivor
    ) THEN
        RAISE EXCEPTION 'promoter_merge names a survivor that is also a loser';
    END IF;
END $$;

-- 1. A missing survivor is one of its losers, renamed. One loser per survivor, or the UNIQUE on
--    slug would refuse the second; the rest move in step 3.
UPDATE promoter p
SET slug = m.survivor, name = m.survivor_name
FROM (
    SELECT survivor, survivor_name, MIN(loser) AS loser
    FROM promoter_merge
    WHERE loser <> survivor
      AND NOT EXISTS (SELECT 1 FROM promoter s WHERE s.slug = promoter_merge.survivor)
    GROUP BY survivor, survivor_name
) m
WHERE p.slug = m.loser;

-- 2. The display name, on every survivor that exists.
UPDATE promoter p
SET name = m.survivor_name
FROM (SELECT DISTINCT survivor, survivor_name FROM promoter_merge) m
WHERE p.slug = m.survivor
  AND p.name <> m.survivor_name;

-- 3. The losers' events, moved. An event already linked to the survivor keeps that link and loses
--    the duplicate in step 4.
UPDATE event_promoter ep
SET promoter_id = s.id
FROM promoter_merge m
JOIN promoter l ON l.slug = m.loser
JOIN promoter s ON s.slug = m.survivor
WHERE m.loser <> m.survivor
  AND ep.promoter_id = l.id
  AND NOT EXISTS (
      SELECT 1 FROM event_promoter d
      WHERE d.event_id = ep.event_id AND d.promoter_id = s.id
  );

-- 4. The losers, gone. The cascade on event_promoter.promoter_id takes their remaining links.
DELETE FROM promoter l
USING promoter_merge m
WHERE l.slug = m.loser
  AND m.loser <> m.survivor
  AND EXISTS (SELECT 1 FROM promoter s WHERE s.slug = m.survivor);
