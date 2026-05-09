package de.norm.events.venue

import de.norm.events.venue.VenueEntity.Companion.fromDomain
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * R2DBC entity mapped to the `venue` table.
 *
 * Kept separate from the core [Venue] domain class so that `events-core` remains
 * free of Spring Data annotations. Conversion functions [toDomain] and [fromDomain]
 * bridge the two representations.
 */
@Table("venue", schema = "events")
data class VenueEntity(
    @Id val id: Long? = null,
    val name: String,
    val slug: String,
    val address: String? = null,
    val city: String = "Berlin",
    val postalCode: String? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val websiteUrl: String? = null,
    val imageUrl: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
) {
    /** Converts this persistence entity to the shared domain model. */
    fun toDomain(): Venue =
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

    companion object {
        /** Creates a persistence entity from the shared domain model. */
        fun fromDomain(venue: Venue): VenueEntity =
            VenueEntity(
                id = venue.id,
                name = venue.name,
                slug = venue.slug,
                address = venue.address,
                city = venue.city,
                postalCode = venue.postalCode,
                latitude = venue.latitude,
                longitude = venue.longitude,
                websiteUrl = venue.websiteUrl,
                imageUrl = venue.imageUrl,
                createdAt = venue.createdAt,
                updatedAt = venue.updatedAt
            )
    }
}
