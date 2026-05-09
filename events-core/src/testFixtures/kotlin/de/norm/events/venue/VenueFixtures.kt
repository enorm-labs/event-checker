package de.norm.events.venue

import java.math.BigDecimal
import java.time.Instant

/**
 * Factory for creating [Venue] instances in tests.
 *
 * Every property has a sensible default so tests only need to specify
 * the fields relevant to the scenario under test.
 *
 * ```kotlin
 * val venue = VenueFixtures.create(name = "Privatclub")
 * ```
 */
object VenueFixtures {
    fun create(
        id: Long? = null,
        name: String = "Astra Kulturhaus",
        slug: String = "astra-kulturhaus",
        address: String? = "Revaler Str. 99",
        city: String = "Berlin",
        postalCode: String? = "10245",
        latitude: BigDecimal? = BigDecimal("52.507242"),
        longitude: BigDecimal? = BigDecimal("13.451803"),
        websiteUrl: String? = "https://www.astra-berlin.de",
        imageUrl: String? = null,
        createdAt: Instant? = null,
        updatedAt: Instant? = null
    ): Venue =
        Venue(
            id = id,
            name = name,
            slug = slug,
            address = address,
            city = city,
            postalCode = postalCode,
            latitude = latitude,
            longitude = longitude,
            websiteUrl = websiteUrl,
            imageUrl = imageUrl,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
