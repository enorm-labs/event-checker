package de.norm.events.promoter

/**
 * Thrown when a promoter with a given ID cannot be found.
 */
class PromoterNotFoundException(
    id: Long
) : RuntimeException("Promoter with id $id not found")
