package de.norm.events.promoter

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * R2DBC entity mapped to the `promoter` table.
 */
@Table("promoter", schema = "events")
data class PromoterEntity(
    @Id val id: Long? = null,
    val name: String,
    val slug: String,
    val websiteUrl: String? = null,
    val imageUrl: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
) {
    fun toDomain(): Promoter =
        Promoter(
            id = id,
            name = name,
            slug = slug,
            websiteUrl = websiteUrl,
            imageUrl = imageUrl,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    companion object {
        fun fromDomain(promoter: Promoter): PromoterEntity =
            PromoterEntity(
                id = promoter.id,
                name = promoter.name,
                slug = promoter.slug,
                websiteUrl = promoter.websiteUrl,
                imageUrl = promoter.imageUrl,
                createdAt = promoter.createdAt,
                updatedAt = promoter.updatedAt
            )
    }
}
