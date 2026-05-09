package de.norm.events.artist

/**
 * Thrown when an artist with a given ID cannot be found.
 */
class ArtistNotFoundException(
    id: Long
) : RuntimeException("Artist with id $id not found")
