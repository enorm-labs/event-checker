package de.norm.events.artist

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * Read-only R2DBC entity mapped to the `artist` table.
 *
 * Lean projection for the BFF's read paths. The table is owned and written by the importer.
 */
@Table("artist")
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
)
