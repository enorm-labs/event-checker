package de.norm.events.scraper

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata declaring the scraper module's allowed dependencies.
 *
 * The scraper module orchestrates importing event data from external venue
 * websites. It needs access to:
 * - `event` — to upsert scraped events via the event repository
 * - `venue` — to resolve venue references for scraped events
 * - `artist` — to resolve or auto-create artists found in scraped lineups
 * - `promoter` — to resolve or auto-create promoters found in scraped events
 * - `slug` — to generate URL-friendly slugs for auto-created entities
 */
@ApplicationModule(allowedDependencies = ["event", "venue", "artist", "promoter", "slug"])
class ScraperModule
