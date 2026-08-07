package de.norm.events.meta

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata for the build-metadata read module. Depends on nothing: it reports facts about
 * the running artifact and touches neither the domain nor the database.
 */
@ApplicationModule(allowedDependencies = [])
class MetaModule
