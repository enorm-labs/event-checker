package de.norm.events.venue

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata for the venue read module. Depends only on the shared `common` module.
 */
@ApplicationModule(allowedDependencies = ["common"])
class VenueModule
