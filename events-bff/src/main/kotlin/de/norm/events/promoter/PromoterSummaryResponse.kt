package de.norm.events.promoter

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Compact promoter representation embedded in event detail responses.
 */
@Schema(description = "Compact promoter summary")
data class PromoterSummaryResponse(
    @Schema(description = "Database primary key", example = "3")
    val id: Long,
    @Schema(description = "URL-friendly identifier", example = "36-concerts")
    val slug: String,
    @Schema(description = "Display name of the promoter", example = "36 Concerts")
    val name: String,
    @Schema(description = "URL of the promoter's website or social page")
    val websiteUrl: String?,
    @Schema(description = "URL of the promoter's logo image")
    val imageUrl: String?
) {
    companion object {
        fun fromEntity(entity: PromoterEntity): PromoterSummaryResponse =
            PromoterSummaryResponse(
                id = requireNotNull(entity.id) { "Persisted promoter must have an ID" },
                slug = entity.slug,
                name = entity.name,
                websiteUrl = entity.websiteUrl,
                imageUrl = entity.imageUrl
            )
    }
}
