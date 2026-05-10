package de.norm.events.scraper

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Admin REST controller for managing event sources and triggering imports.
 *
 * Provides endpoints for event source CRUD, manual import triggers, and
 * retry operations. All endpoints live under `/api/admin/event-sources`.
 */
@RestController
@RequestMapping("/api/admin/event-sources")
@Tag(name = "Admin: Event Sources", description = "Endpoints for managing event sources and triggering imports")
class EventSourceController(
    private val eventImportService: EventImportService,
    private val eventSourceService: EventSourceService
) {
    // -- Import operations ---

    /**
     * Triggers an import for all enabled event sources.
     *
     * Each source is processed independently — a failure in one does not
     * prevent others from being imported.
     */
    @PostMapping("/import")
    @Operation(summary = "Trigger import for all enabled event sources")
    suspend fun importAll(): List<ImportResultResponse> = eventImportService.importAll()

    /**
     * Triggers an import for a single event source identified by its slug.
     *
     * @throws EventSourceNotFoundException if no source with the given slug exists.
     */
    @PostMapping("/{slug}/import")
    @Operation(summary = "Trigger import for a single event source by slug")
    suspend fun importBySlug(
        @PathVariable slug: String
    ): ImportResultResponse = eventImportService.importBySlug(slug)

    // -- CRUD operations ---

    /**
     * Lists all event sources with their current status and scheduling metadata.
     *
     * Pagination and sorting are controlled by query parameters
     * (e.g. `?page=0&size=20&sort=name,asc`).
     */
    @GetMapping
    @Operation(summary = "List all event sources with their status")
    fun listSources(
        @PageableDefault(size = 20, sort = ["name"]) pageable: Pageable
    ): Flow<EventSourceResponse> = eventSourceService.findAll(pageable)

    /**
     * Creates a new event source.
     *
     * The slug is auto-generated from the source name using [de.norm.events.slug.SlugGenerator].
     * The source starts in IDLE status, ready for the scheduler to pick up.
     *
     * @return the created event source with generated slug and default metadata.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new event source")
    suspend fun createSource(
        @Valid @RequestBody request: EventSourceCreateRequest
    ): EventSourceResponse = eventSourceService.create(request)

    /**
     * Retrieves a single event source by its slug.
     *
     * @throws EventSourceNotFoundException if no source with the given slug exists.
     */
    @GetMapping("/{slug}")
    @Operation(summary = "Get a single event source by slug")
    suspend fun getSource(
        @PathVariable slug: String
    ): EventSourceResponse = eventSourceService.findBySlug(slug)

    /**
     * Partially updates an event source's configuration.
     *
     * Supports toggling `enabled`, changing `importIntervalMinutes`, and adjusting `maxRetries`.
     * Only non-null fields in the request body are applied.
     *
     * @throws EventSourceNotFoundException if no source with the given slug exists.
     */
    @PatchMapping("/{slug}")
    @Operation(summary = "Update event source configuration (enable/disable, interval, retries)")
    suspend fun updateSource(
        @PathVariable slug: String,
        @Valid @RequestBody request: EventSourceUpdateRequest
    ): EventSourceResponse = eventSourceService.update(slug, request)

    // -- Retry operations ---

    /**
     * Resets all failed event sources back to IDLE for immediate retry.
     *
     * Useful after fixing a network issue or scraper bug — all failed sources
     * are picked up by the scheduler on the next tick.
     */
    @PostMapping("/retry")
    @Operation(summary = "Reset all failed event sources for immediate retry")
    suspend fun retryAll(): Map<String, Int> = mapOf("resetCount" to eventSourceService.retryAll())

    /**
     * Resets a single failed event source for immediate retry.
     *
     * Clears the error state, resets `retryCount` to 0, and sets status back to IDLE
     * so the scheduler will pick it up on the next tick.
     *
     * @throws EventSourceNotFoundException if no source with the given slug exists.
     */
    @PostMapping("/{slug}/retry")
    @Operation(summary = "Reset a failed event source for immediate retry")
    suspend fun retrySource(
        @PathVariable slug: String
    ): EventSourceResponse = eventSourceService.retry(slug)

    // -- Delete ---

    /**
     * Deletes an event source by its slug.
     *
     * @throws EventSourceNotFoundException if no source with the given slug exists.
     */
    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an event source by slug")
    suspend fun deleteSource(
        @PathVariable slug: String
    ) {
        eventSourceService.delete(slug)
    }
}
