package de.norm.events.artist

import java.time.Instant

/**
 * Factory for creating [Artist] instances in tests.
 *
 * ```kotlin
 * val artist = ArtistFixtures.create(name = "Maid of Ace")
 * ```
 */
object ArtistFixtures {
    fun create(
        id: Long? = null,
        name: String = "The Adicts",
        slug: String = "the-adicts",
        description: String? = "Formed in Ipswich in the late 1970s",
        imageUrl: String? = null,
        websiteUrl: String? = "https://theadicts.net/",
        facebookUrl: String? = null,
        instagramUrl: String? = null,
        youtubeUrl: String? = null,
        createdAt: Instant? = null,
        updatedAt: Instant? = null
    ): Artist =
        Artist(
            id = id,
            name = name,
            slug = slug,
            description = description,
            imageUrl = imageUrl,
            websiteUrl = websiteUrl,
            facebookUrl = facebookUrl,
            instagramUrl = instagramUrl,
            youtubeUrl = youtubeUrl,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
