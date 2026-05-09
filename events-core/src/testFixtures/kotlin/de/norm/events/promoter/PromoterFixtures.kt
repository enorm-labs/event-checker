package de.norm.events.promoter

import java.time.Instant

/**
 * Factory for creating [Promoter] instances in tests.
 *
 * ```kotlin
 * val promoter = PromoterFixtures.create(name = "Goodlive")
 * ```
 */
object PromoterFixtures {
    fun create(
        id: Long? = null,
        name: String = "36 Concerts",
        slug: String = "36-concerts",
        websiteUrl: String? = "https://www.facebook.com/36Concerts/",
        imageUrl: String? = null,
        createdAt: Instant? = null,
        updatedAt: Instant? = null
    ): Promoter =
        Promoter(
            id = id,
            name = name,
            slug = slug,
            websiteUrl = websiteUrl,
            imageUrl = imageUrl,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
