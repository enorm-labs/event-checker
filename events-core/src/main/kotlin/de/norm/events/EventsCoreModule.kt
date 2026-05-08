package de.norm.events

import org.springframework.modulith.Modulithic

/**
 * Marker class that serves as the root package anchor for Spring Modulith module detection in the events-core library.
 *
 * Since events-core is a plain library (no Spring Boot application class), the @Modulithic annotation tells Spring Modulith to treat this class as the root
 * for module scanning. Each direct sub-package will be considered an application module.
 */
@Modulithic
class EventsCoreModule
