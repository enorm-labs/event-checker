-- Two promoter rows Huxleys credits with a two-letter initial in front of a descriptor (#307):
-- "JB Freie Musik presents" lost "Musik" to the descriptor strip and its initials to the de-shout,
-- and was stored as `Jb Freie`; "JM Audio Entertainment" the same way, as `Jm Audio`. The
-- normalizer now pins both trading names; this renames the rows that exist, since a row keeps the
-- name whichever import minted it. Keyed on slug and a no-op where a row is absent, and the
-- survivor slug is the slug of the new name, which V025 is the reason to check.
--
-- Unqualified table name, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).

CREATE TEMPORARY TABLE promoter_merge (
    loser         TEXT NOT NULL,
    survivor      TEXT NOT NULL,
    survivor_name TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO promoter_merge (loser, survivor, survivor_name) VALUES
    ('jb-freie', 'jb-freie-musik',         'JB Freie Musik'),
    ('jm-audio', 'jm-audio-entertainment', 'JM Audio Entertainment');

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

UPDATE promoter p
SET name = m.survivor_name
FROM (SELECT DISTINCT survivor, survivor_name FROM promoter_merge) m
WHERE p.slug = m.survivor
  AND p.name <> m.survivor_name;

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

DELETE FROM promoter l
USING promoter_merge m
WHERE l.slug = m.loser
  AND m.loser <> m.survivor
  AND EXISTS (SELECT 1 FROM promoter s WHERE s.slug = m.survivor);
