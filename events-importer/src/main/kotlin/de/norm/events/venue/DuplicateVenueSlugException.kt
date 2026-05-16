package de.norm.events.venue

/**
 * Thrown when attempting to create or update a venue with a name that generates
 * a slug already used by another venue. Slugs are derived deterministically from
 * names via [de.norm.events.slug.SlugGenerator], so different names can collide
 * (e.g. "Über Cool" and "Uber Cool" both produce "uber-cool").
 */
class DuplicateVenueSlugException(
    name: String,
    slug: String
) : RuntimeException("A venue with slug '$slug' already exists (generated from name '$name')")
