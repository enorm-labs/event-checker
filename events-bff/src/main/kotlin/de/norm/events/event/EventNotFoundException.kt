package de.norm.events.event

/**
 * Thrown when an event with a given slug cannot be found.
 */
class EventNotFoundException(
    slug: String
) : RuntimeException("Event with slug '$slug' not found")
