package de.norm.events.promoter

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata declaring the promoter module as self-contained with no
 * dependencies on other application modules.
 */
@ApplicationModule(allowedDependencies = [])
class PromoterModule
