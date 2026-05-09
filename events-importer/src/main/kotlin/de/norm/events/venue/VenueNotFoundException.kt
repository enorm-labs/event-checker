package de.norm.events.venue

/**
 * Thrown when a venue with a given ID cannot be found.
 */
class VenueNotFoundException(
    id: Long
) : RuntimeException("Venue with id $id not found")
