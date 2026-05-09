package de.norm.events.artist

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata declaring the artist module as self-contained with no
 * dependencies on other application modules.
 */
@ApplicationModule(allowedDependencies = [])
class ArtistModule
