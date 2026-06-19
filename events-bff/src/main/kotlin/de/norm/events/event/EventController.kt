package de.norm.events.event

import de.norm.events.common.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Public read API for events: filtered search, today's events, calendar range, and detail by slug.
 */
@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Public endpoints for browsing, filtering, and viewing events")
class EventController(
    private val eventService: EventService
) {
    @GetMapping
    @Operation(summary = "Search events with optional filters and pagination")
    @Suppress("LongParameterList")
    suspend fun list(
        @Parameter(description = "Earliest event date (inclusive), ISO-8601 (e.g. 2026-06-19). Defaults to today when both from/to are omitted.")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate?,
        @Parameter(description = "Latest event date (inclusive), ISO-8601 (e.g. 2026-06-30).")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate?,
        @Parameter(description = "Event type filter, e.g. CONCERT (case-insensitive).")
        @RequestParam(required = false)
        eventType: String?,
        @Parameter(description = "Venue slug filter — only events at the matching venue.")
        @RequestParam(required = false)
        venue: String?,
        @Parameter(description = "Artist slug filter — only events featuring the matching artist.")
        @RequestParam(required = false)
        artist: String?,
        @Parameter(description = "Promoter slug filter — only events from the matching promoter.")
        @RequestParam(required = false)
        promoter: String?,
        @Parameter(description = "Genre tag slug filter — only events tagged with the matching genre.")
        @RequestParam(required = false)
        genre: String?,
        @Parameter(description = "Minimum presale price (inclusive). Excludes events with an unknown (null) price.")
        @RequestParam(required = false)
        minPrice: BigDecimal?,
        @Parameter(description = "Maximum presale price (inclusive). Excludes events with an unknown (null) price.")
        @RequestParam(required = false)
        maxPrice: BigDecimal?,
        @Parameter(description = "Case-insensitive substring search over the event title and subtitle.")
        @RequestParam(required = false)
        q: String?,
        @ParameterObject
        @PageableDefault(size = 20, sort = ["eventDate"])
        pageable: Pageable
    ): PageResponse<EventSummaryResponse> =
        eventService.search(
            EventFilter(
                from = from,
                to = to,
                eventType = eventType,
                venueSlug = venue,
                artistSlug = artist,
                promoterSlug = promoter,
                genreSlug = genre,
                minPrice = minPrice,
                maxPrice = maxPrice,
                query = q
            ),
            pageable
        )

    @GetMapping("/today")
    @Operation(summary = "Get today's events")
    suspend fun today(): List<EventSummaryResponse> = eventService.today()

    @GetMapping("/calendar")
    @Operation(summary = "Get events within an inclusive date range for the calendar view")
    suspend fun calendar(
        @Parameter(description = "Range start date (inclusive), ISO-8601.", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        from: LocalDate,
        @Parameter(description = "Range end date (inclusive), ISO-8601. Must not precede 'from' or exceed 92 days from it.", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        to: LocalDate
    ): List<EventSummaryResponse> = eventService.calendar(from, to)

    @GetMapping("/{slug}")
    @Operation(summary = "Get a single event by slug")
    suspend fun findBySlug(
        @Parameter(description = "Unique event slug (format: {date}-{venue}-{title}).", example = "2026-06-18-lido-sam-prekop-john-mcentire", required = true)
        @PathVariable slug: String
    ): EventDetailResponse = eventService.findBySlug(slug)
}
