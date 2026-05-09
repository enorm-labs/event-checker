package de.norm.events.event

/**
 * Thrown when an event with a given ID cannot be found.
 */
class EventNotFoundException(
    id: Long
) : RuntimeException("Event with id $id not found")
