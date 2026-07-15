package de.norm.events.venue

import de.norm.events.common.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Public read API for venues.
 */
@RestController
@RequestMapping("/venues")
@Tag(name = "Venues", description = "Public endpoints for browsing venues")
class VenueController(
    private val venueService: VenueService
) {
    @GetMapping
    @Operation(summary = "List venues with pagination and optional name search")
    suspend fun list(
        @Parameter(description = "Case-insensitive substring filter on the venue name. Omitted/blank returns all venues.")
        @RequestParam(required = false)
        q: String?,
        @Parameter(
            description =
                "District filter — only venues in the matching Berlin borough (e.g. friedrichshain-kreuzberg). " +
                    "Omitted/blank returns all districts."
        )
        @RequestParam(required = false)
        district: String?,
        @ParameterObject
        @PageableDefault(size = 20, sort = ["name"])
        pageable: Pageable
    ): PageResponse<VenueSummaryResponse> = venueService.list(q, district, pageable)

    @GetMapping("/{slug}")
    @Operation(summary = "Get a single venue by slug")
    suspend fun findBySlug(
        @Parameter(description = "Unique venue slug.", example = "lido", required = true)
        @PathVariable slug: String
    ): VenueDetailResponse = venueService.findBySlug(slug)
}
