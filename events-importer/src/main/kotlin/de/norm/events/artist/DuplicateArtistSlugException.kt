package de.norm.events.artist

/**
 * Thrown when attempting to create or update an artist with a name that generates
 * a slug already used by another artist. Slugs are derived deterministically from
 * names via [de.norm.events.slug.SlugGenerator], so different names can collide
 * (e.g. "Motörhead" and "Motorhead" both produce "motorhead").
 */
class DuplicateArtistSlugException(
    name: String,
    slug: String
) : RuntimeException("An artist with slug '$slug' already exists (generated from name '$name')")
