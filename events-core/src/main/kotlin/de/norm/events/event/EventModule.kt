package de.norm.events.event

import org.springframework.modulith.ApplicationModule

/**
 * Module metadata declaring the event module's allowed dependencies.
 *
 * The event domain model references artist, venue, and promoter domain
 * objects as part of its aggregate (e.g. [Event.venue], [Event.lineup],
 * [Event.promoters]).
 */
@ApplicationModule(allowedDependencies = ["artist", "venue", "promoter"])
class EventModule
