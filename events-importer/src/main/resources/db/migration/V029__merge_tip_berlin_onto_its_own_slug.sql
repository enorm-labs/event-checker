-- V023 makes a missing survivor out of its first-listed loser without checking that this loser
-- exists. Production had `tip-berlin` and not `tip`, so `tipberlin` was never made, `tip-berlin`
-- was neither renamed nor deleted, and its slug is not the slug of "tipBerlin" — the next import
-- that meets the credit mints a second row beside it (#1343, the trap V025 repaired for greyzone).
-- This moves the row onto its own slug, and step 1 here picks the smallest loser that exists.
-- `stand-in-front` was minted before the de-shout and keeps "Stand In Front"; a rename only.
-- The same four-step shape as V023, keyed on slug and a no-op where a row is absent.
--
-- Unqualified table names, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).

CREATE TEMPORARY TABLE promoter_merge (
    loser         TEXT NOT NULL,
    survivor      TEXT NOT NULL,
    survivor_name TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO promoter_merge (loser, survivor, survivor_name) VALUES
    ('tip',            'tipberlin',      'tipBerlin'),
    ('tip-berlin',     'tipberlin',      'tipBerlin'),
    ('stand-in-front', 'stand-in-front', 'Stand in Front');

-- 1. A missing survivor is the smallest of its losers that exists, renamed.
UPDATE promoter p
SET slug = m.survivor, name = m.survivor_name
FROM (
    SELECT survivor, survivor_name, MIN(loser) AS loser
    FROM promoter_merge
    WHERE loser <> survivor
      AND EXISTS (SELECT 1 FROM promoter l WHERE l.slug = promoter_merge.loser)
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

-- 3. The losers' events, moved.
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

-- 4. The losers, gone.
DELETE FROM promoter l
USING promoter_merge m
WHERE l.slug = m.loser
  AND m.loser <> m.survivor
  AND EXISTS (SELECT 1 FROM promoter s WHERE s.slug = m.survivor);
