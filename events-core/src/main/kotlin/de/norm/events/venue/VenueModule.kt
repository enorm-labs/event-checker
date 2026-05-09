package de.norm.events.venue

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata declaring the venue module as self-contained with no
 * dependencies on other application modules.
 */
@ApplicationModule(allowedDependencies = [])
class VenueModule
