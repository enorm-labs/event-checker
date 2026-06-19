package de.norm.events.promoter

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * Read-only R2DBC entity mapped to the `promoter` table.
 *
 * Lean projection for the BFF. Promoters have no dedicated page; they appear embedded in
 * event detail responses and as an event filter. The table is owned/written by the importer.
 */
@Table("promoter")
data class PromoterEntity(
    @Id val id: Long? = null,
    val name: String,
    val slug: String,
    val websiteUrl: String? = null,
    val imageUrl: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
