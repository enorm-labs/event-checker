package de.norm.events.artist

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata for the artist read module. Depends only on the shared `common` module.
 */
@ApplicationModule(allowedDependencies = ["common"])
class ArtistModule
