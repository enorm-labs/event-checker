package de.norm.events.genretag

/**
 * Thrown when a genre tag with the requested ID cannot be found.
 */
class GenreTagNotFoundException(
    id: Long
) : RuntimeException("Genre tag with id $id not found")
