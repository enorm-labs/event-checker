package de.norm.events.promoter

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata for the promoter read module. Depends only on the shared `common` module.
 */
@ApplicationModule(allowedDependencies = ["common"])
class PromoterModule
