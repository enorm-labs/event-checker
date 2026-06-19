package de.norm.events.genretag

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata for the genre tag read module. Depends only on the shared `common` module.
 */
@ApplicationModule(allowedDependencies = ["common"])
class GenreTagModule
