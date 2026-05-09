package de.norm.events.slug

import com.github.slugify.Slugify

/**
 * Shared singleton for URL-friendly slug generation.
 *
 * [Slugify] is stateless and thread-safe, so a single instance is reused
 * across all services instead of each service creating its own.
 */
object SlugGenerator {
    private val slugify = Slugify.builder().build()

    /** Converts [input] into a URL-friendly slug (e.g. "My Event" → "my-event"). */
    fun slugify(input: String): String = slugify.slugify(input)
}
