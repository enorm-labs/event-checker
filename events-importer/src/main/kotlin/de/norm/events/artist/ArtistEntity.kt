package de.norm.events.artist

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * R2DBC entity mapped to the `artist` table.
 *
 * Kept separate from the core [Artist] domain class so that `events-core` remains
 * free of Spring Data annotations.
 */
@Table("artist", schema = "events")
data class ArtistEntity(
    @Id val id: Long? = null,
    val name: String,
    val slug: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val websiteUrl: String? = null,
    val facebookUrl: String? = null,
    val instagramUrl: String? = null,
    val youtubeUrl: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
) {
    fun toDomain(): Artist =
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

    companion object {
        fun fromDomain(artist: Artist): ArtistEntity =
            ArtistEntity(
                id = artist.id,
                name = artist.name,
                slug = artist.slug,
                description = artist.description,
                imageUrl = artist.imageUrl,
                websiteUrl = artist.websiteUrl,
                facebookUrl = artist.facebookUrl,
                instagramUrl = artist.instagramUrl,
                youtubeUrl = artist.youtubeUrl,
                createdAt = artist.createdAt,
                updatedAt = artist.updatedAt
            )
    }
}
