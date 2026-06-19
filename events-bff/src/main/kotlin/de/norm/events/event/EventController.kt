package de.norm.events.event

import de.norm.events.common.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) eventType: String?,
        @RequestParam(required = false) venue: String?,
        @RequestParam(required = false) artist: String?,
        @RequestParam(required = false) promoter: String?,
        @RequestParam(required = false) genre: String?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) q: String?,
        @PageableDefault(size = 20, sort = ["eventDate"]) pageable: Pageable
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
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): List<EventSummaryResponse> = eventService.calendar(from, to)

    @GetMapping("/{slug}")
    @Operation(summary = "Get a single event by slug")
    suspend fun findBySlug(
        @PathVariable slug: String
    ): EventDetailResponse = eventService.findBySlug(slug)
}
