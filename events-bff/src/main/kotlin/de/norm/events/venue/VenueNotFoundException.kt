package de.norm.events.venue

/**
 * Thrown when a venue with a given slug cannot be found.
 */
class VenueNotFoundException(
    slug: String
) : RuntimeException("Venue with slug '$slug' not found")
