package de.norm.events.common

import org.springframework.modulith.ApplicationModule

/**
 * Marks the `common` package as an OPEN Spring Modulith module, allowing any feature
 * module to depend on its shared types (e.g. [PageResponse]) without declaring an
 * explicit dependency. Shared, dependency-free API plumbing lives here.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
class CommonModule
