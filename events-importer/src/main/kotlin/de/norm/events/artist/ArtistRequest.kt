package de.norm.events.artist

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request body for creating or updating an artist.
 *
 * Only includes user-provided fields — `id`, `createdAt`, and `updatedAt`
 * are managed by the database. For updates, the `id` is taken from the path parameter.
 * Slugs are a server-side concern computed by the service layer, so they are not
 * part of the request DTO.
 */
@Schema(description = "Request body for creating or updating an artist")
data class ArtistRequest(
    @field:NotBlank(message = "Artist name must not be blank")
    @field:Size(max = 255, message = "Artist name must not exceed 255 characters")
    @Schema(description = "Stage name or band name", example = "The Adicts", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
    @field:Size(max = 10000, message = "Description must not exceed 10000 characters")
    @Schema(description = "Biography or description text", example = "Formed in Ipswich in the late 1970s…")
    val description: String? = null,
    @field:Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    @Schema(description = "URL of the artist's photo or band logo", example = "https://example.com/adicts.jpg")
    val imageUrl: String? = null,
    @field:Size(max = 2048, message = "Website URL must not exceed 2048 characters")
    @Schema(description = "URL of the artist's official homepage", example = "https://theadicts.net/")
    val websiteUrl: String? = null,
    @field:Size(max = 2048, message = "Facebook URL must not exceed 2048 characters")
    @Schema(description = "URL of the artist's Facebook page", example = "https://www.facebook.com/theadicts")
    val facebookUrl: String? = null,
    @field:Size(max = 2048, message = "Instagram URL must not exceed 2048 characters")
    @Schema(description = "URL of the artist's Instagram profile", example = "https://www.instagram.com/theadictsofficial/")
    val instagramUrl: String? = null,
    @field:Size(max = 2048, message = "YouTube URL must not exceed 2048 characters")
    @Schema(description = "URL of the artist's YouTube channel", example = "https://www.youtube.com/@theadictsofficial")
    val youtubeUrl: String? = null
)
