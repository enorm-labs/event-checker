package de.norm.events.artist

/**
 * Thrown when an artist with a given slug cannot be found.
 */
class ArtistNotFoundException(
    slug: String
) : RuntimeException("Artist with slug '$slug' not found")
