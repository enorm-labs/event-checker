-- V1: Initial data model for event-checker
-- Designed to capture music event data from Berlin venue websites
-- (e.g. Astra Kulturhaus, Badehaus Berlin, Cassiopeia, Privatclub).

-- Create a dedicated schema to isolate application tables from the default public schema.
CREATE SCHEMA IF NOT EXISTS events;

-- ============================================================
-- VENUES
-- Represents a physical location where events take place.
-- ============================================================
CREATE TABLE venue
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL UNIQUE,
    address     TEXT,
    city        TEXT        NOT NULL DEFAULT 'Berlin',
    postal_code TEXT,
    latitude    DECIMAL(9, 6),
    longitude   DECIMAL(9, 6),
    website_url TEXT,
    image_url   TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- No explicit slug index needed — the UNIQUE constraint already creates an implicit index.

-- ============================================================
-- ARTISTS
-- Represents a musical artist or band that performs at events.
-- ============================================================
CREATE TABLE artist
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          TEXT        NOT NULL,
    slug          TEXT        NOT NULL UNIQUE,
    description   TEXT,
    image_url     TEXT,
    website_url   TEXT,
    facebook_url  TEXT,
    instagram_url TEXT,
    youtube_url   TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- No explicit slug index needed — the UNIQUE constraint already creates an implicit index.

-- ============================================================
-- PROMOTERS
-- Represents an event promoter / presenter (e.g. "36 Concerts").
-- ============================================================
CREATE TABLE promoter
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        TEXT        NOT NULL,
    slug        TEXT        NOT NULL UNIQUE,
    website_url TEXT,
    image_url   TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- No explicit slug index needed — the UNIQUE constraint already creates an implicit index.

-- ============================================================
-- EVENTS
-- Core entity: a single event at a venue on a specific date.
-- source_id uniquely identifies the event from its import source
-- to enable idempotent imports (upsert).
-- ============================================================
CREATE TABLE event
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id           BIGINT      NOT NULL REFERENCES venue (id),
    title              TEXT        NOT NULL,
    subtitle           TEXT,
    description        TEXT,
    event_type         TEXT        NOT NULL DEFAULT 'CONCERT',
    status             TEXT        NOT NULL DEFAULT 'SCHEDULED',
    slug               TEXT        NOT NULL UNIQUE,
    event_date         DATE        NOT NULL,
    doors_time         TIME,
    start_time         TIME,
    image_url          TEXT,
    source_url         TEXT,
    source_id          TEXT        NOT NULL UNIQUE,
    ticket_url         TEXT,
    facebook_event_url TEXT,
    genre              TEXT,
    price_presale      DECIMAL(10, 2),
    price_box_office   DECIMAL(10, 2),
    price_currency     TEXT        NOT NULL DEFAULT 'EUR',
    price_note         TEXT,
    sold_out           BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_event_venue_id ON event (venue_id);
CREATE INDEX idx_event_event_date ON event (event_date);
-- No explicit slug index needed — the UNIQUE constraint already creates an implicit index.

-- ============================================================
-- EVENT_ARTIST (join table)
-- Links events to artists with role and billing order.
-- ============================================================
CREATE TABLE event_artist
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id      BIGINT NOT NULL REFERENCES event (id) ON DELETE CASCADE,
    artist_id     BIGINT NOT NULL REFERENCES artist (id) ON DELETE CASCADE,
    role          TEXT   NOT NULL DEFAULT 'HEADLINER',
    billing_order INT    NOT NULL DEFAULT 0,
    UNIQUE (event_id, artist_id)
);

CREATE INDEX idx_event_artist_event_id ON event_artist (event_id);
CREATE INDEX idx_event_artist_artist_id ON event_artist (artist_id);

-- ============================================================
-- EVENT_PROMOTER (join table)
-- Links events to their promoters/presenters.
-- ============================================================
CREATE TABLE event_promoter
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id    BIGINT NOT NULL REFERENCES event (id) ON DELETE CASCADE,
    promoter_id BIGINT NOT NULL REFERENCES promoter (id) ON DELETE CASCADE,
    UNIQUE (event_id, promoter_id)
);

CREATE INDEX idx_event_promoter_promoter_id ON event_promoter (promoter_id);

-- ============================================================
-- TRIGGER: auto-update updated_at on row modification
-- A database-level trigger is more reliable than application-level
-- auditing since it catches all update paths (including raw SQL).
-- ============================================================
CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_venue_updated_at
    BEFORE UPDATE
    ON venue
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_artist_updated_at
    BEFORE UPDATE
    ON artist
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_promoter_updated_at
    BEFORE UPDATE
    ON promoter
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_event_updated_at
    BEFORE UPDATE
    ON event
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
