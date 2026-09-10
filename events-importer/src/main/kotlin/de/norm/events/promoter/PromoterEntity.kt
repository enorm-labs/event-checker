package de.norm.events.promoter

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * R2DBC entity mapped to the `promoter` table.
 */
@Table("promoter")
data class PromoterEntity(
    @Id val id: Long? = null,
    val name: String,
    val slug: String,
    val websiteUrl: String? = null,
    val imageUrl: String? = null,
    val imageAttribution: String? = null,
    val imageLicenceId: String? = null,
    val imageSourceUrl: String? = null,
    @CreatedDate val createdAt: Instant? = null,
    @LastModifiedDate val updatedAt: Instant? = null
) {
    fun toDomain(): Promoter =
        Promoter(
            id = id,
            name = name,
            slug = slug,
            websiteUrl = websiteUrl,
            imageUrl = imageUrl,
            imageAttribution = imageAttribution,
            imageLicenceId = imageLicenceId,
            imageSourceUrl = imageSourceUrl,
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
                imageAttribution = promoter.imageAttribution,
                imageLicenceId = promoter.imageLicenceId,
                imageSourceUrl = promoter.imageSourceUrl,
                createdAt = promoter.createdAt,
                updatedAt = promoter.updatedAt
            )
    }
}
