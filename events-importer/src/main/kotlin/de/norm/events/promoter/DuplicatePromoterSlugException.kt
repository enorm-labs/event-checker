package de.norm.events.promoter

/**
 * Thrown when attempting to create or update a promoter with a name that generates
 * a slug already used by another promoter. Slugs are derived deterministically from
 * names via [de.norm.events.slug.SlugGenerator], so different names can collide
 * (e.g. "Über Cool" and "Uber Cool" both produce "uber-cool").
 */
class DuplicatePromoterSlugException(
    name: String,
    slug: String
) : RuntimeException("A promoter with slug '$slug' already exists (generated from name '$name')")
