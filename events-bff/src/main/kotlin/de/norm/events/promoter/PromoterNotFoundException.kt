package de.norm.events.promoter

/**
 * Thrown when a promoter with a given slug cannot be found.
 */
class PromoterNotFoundException(
    slug: String
) : RuntimeException("Promoter with slug '$slug' not found")
