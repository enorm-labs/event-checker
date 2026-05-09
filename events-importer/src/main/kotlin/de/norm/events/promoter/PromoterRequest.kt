package de.norm.events.promoter

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request body for creating or updating a promoter.
 *
 * Only includes user-provided fields — `id`, `createdAt`, and `updatedAt`
 * are managed by the database. For updates, the `id` is taken from the path parameter.
 * Slugs are a server-side concern computed by the service layer, so they are not
 * part of the request DTO.
 */
@Schema(description = "Request body for creating or updating a promoter")
data class PromoterRequest(
    @field:NotBlank(message = "Promoter name must not be blank")
    @field:Size(max = 255, message = "Promoter name must not exceed 255 characters")
    @Schema(description = "Display name of the promoter", example = "36 Concerts", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Size(max = 2048, message = "Website URL must not exceed 2048 characters")
    @Schema(description = "URL of the promoter's website or social page", example = "https://www.facebook.com/36Concerts/")
    val websiteUrl: String? = null,
    @field:Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    @Schema(description = "URL of the promoter's logo image", example = "https://example.com/36-concerts-logo.jpg")
    val imageUrl: String? = null
)
