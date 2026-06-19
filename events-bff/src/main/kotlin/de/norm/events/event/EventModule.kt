package de.norm.events.event

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata for the event read module. Events embed venue, artist, promoter, and genre
 * tag summaries, so it depends on those modules plus the shared `common` module.
 */
@ApplicationModule(allowedDependencies = ["common", "venue", "artist", "promoter", "genretag"])
class EventModule
