package de.norm.events.scraper

import de.norm.events.venue.VenueRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.Clock
import java.time.Instant

/**
 * Orchestrates the event import pipeline: delegate to importer → upsert → cleanup.
 *
 * For each enabled [EventSourceEntity], the service:
 * 1. Resolves the matching [EventImporter] by [EventSource] enum.
 * 2. Delegates fetching and parsing to the importer.
 * 3. Delegates persistence (upsert, artist resolution, stale cleanup) to [EventUpsertService].
 * 4. Updates the event source metadata (status, event count, ETag, etc.).
 */
@Service
class EventImportService(
    private val eventSourceRepository: EventSourceRepository,
    private val eventUpsertService: EventUpsertService,
    private val eventImporters: List<EventImporter>,
    private val venueRepository: VenueRepository,
    /** Programmatic transaction control — used instead of @Transactional to avoid self-invocation issues. */
    private val transactionalOperator: TransactionalOperator,
    /** Injected clock for deterministic time in tests. Defaults to system UTC clock in production. */
    private val clock: Clock = Clock.systemUTC(),
    /**
     * Maximum number of event sources imported concurrently. Each source runs in its
     * own coroutine, bounded by a [Semaphore] to limit database and network pressure.
     * Per-host politeness is already enforced by [PerHostThrottlingFilter], so sources
     * targeting different hosts benefit from true concurrency while same-host sources
     * are naturally serialized at the HTTP layer.
     */
    @Value($$"${app.import.max-concurrency:4}")
    private val maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY
) {
    private val logger = KotlinLogging.logger {}

    /** Index of event importers by their event source for O(1) dispatch. */
    private val importersBySource: Map<EventSource, EventImporter> by lazy {
        eventImporters.associateBy { it.eventSource }
    }

    /**
     * Imports events from all enabled event sources.
     *
     * Sources are imported concurrently (up to [maxConcurrency] at a time).
     * Each source is processed independently — a failure in one source does not
     * prevent other sources from being imported.
     */
    suspend fun importAll(): List<ImportResultResponse> {
        val sources = eventSourceRepository.findByEnabledTrue().toList()
        logger.info { "Starting import for ${sources.size} enabled source(s)" }
        return importConcurrently(sources)
    }

    /**
     * Imports events from a single event source identified by [slug].
     *
     * @throws EventSourceNotFoundException if no source with the given slug exists.
     */
    suspend fun importBySlug(slug: String): ImportResultResponse {
        val source = eventSourceRepository.findBySlug(slug) ?: throw EventSourceNotFoundException(slug)
        return importFromSource(source)
    }

    /**
     * Imports multiple sources concurrently, bounded by [maxConcurrency].
     *
     * Each source runs in its own coroutine, with a [Semaphore] limiting how many
     * execute simultaneously. This is safe because:
     * - The artist cache in [EventUpsertService] is local to each [importFromSource] call.
     * - Concurrent artist creation is handled via [DataIntegrityViolationException] fallback.
     * - Per-host HTTP politeness is enforced by [PerHostThrottlingFilter].
     * - Each source's upsert runs in its own transaction.
     */
    internal suspend fun importConcurrently(sources: List<EventSourceEntity>): List<ImportResultResponse> =
        coroutineScope {
            val semaphore = Semaphore(maxConcurrency)
            sources
                .map { source ->
                    async {
                        semaphore.withPermit {
                            logger.info { "Importing source '${source.slug}' (interval=${source.importIntervalMinutes}min, retries=${source.retryCount})" }
                            importFromSource(source)
                        }
                    }
                }.awaitAll()
        }

    /**
     * Core import pipeline for a single source.
     *
     * Handles the full lifecycle: delegate to importer → upsert → update metadata.
     * Errors are caught and recorded on the event source rather than propagated.
     *
     * Status updates (markRunning/markSuccess/markFailed) run outside the
     * transactional boundary so they always commit, even if a DB error during
     * upsert marks the transaction as rollback-only.
     *
     * **Precondition**: The [source] must be a persisted entity fetched from the repository.
     * This method manages the source's import lifecycle status (RUNNING → SUCCESS/FAILED),
     * so callers must not manipulate the source's status independently.
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount") // Intentional: record any failure; multiple early returns for error paths
    internal suspend fun importFromSource(source: EventSourceEntity): ImportResultResponse {
        requireNotNull(source.id) { "Event source must be persisted (have a non-null id) before importing" }

        val eventSourceEnum =
            try {
                EventSource.valueOf(source.sourceType)
            } catch (_: IllegalArgumentException) {
                val error = "Unknown source type '${source.sourceType}'"
                logger.error { error }
                // Configuration error — will never self-resolve on retry, so mark as MISCONFIGURED
                // instead of FAILED to avoid consuming retry budget (see review issue #1).
                markMisconfigured(source, error)
                return ImportResultResponse(sourceSlug = source.slug, imported = false, eventCount = 0, error = error)
            }

        val importer = importersBySource[eventSourceEnum]
        if (importer == null) {
            val error = "No importer registered for source type '${source.sourceType}'"
            logger.error { error }
            // Configuration error — no importer is deployed for this source type.
            markMisconfigured(source, error)
            return ImportResultResponse(sourceSlug = source.slug, imported = false, eventCount = 0, error = error)
        }

        val runningSource = markRunning(source)

        return try {
            when (val result = importer.importEvents(runningSource.url, runningSource.etag, runningSource.lastModified)) {
                is ImportResult.NotModified -> {
                    logger.info { "Source '${runningSource.slug}' not modified, skipping import" }
                    markSuccess(runningSource, 0)
                    ImportResultResponse(sourceSlug = runningSource.slug, imported = false, eventCount = 0)
                }

                is ImportResult.Success -> {
                    logger.info { "Scraped ${result.events.size} event(s) from '${runningSource.slug}'" }

                    // Look up the venue slug for inclusion in event slugs (ensures cross-venue uniqueness).
                    val venue =
                        venueRepository.findById(runningSource.venueId)
                            ?: error("Venue with id ${runningSource.venueId} not found for source '${runningSource.slug}'")

                    // Wrap upserts and cleanup in a transaction so partial failures roll back cleanly.
                    // Uses TransactionalOperator instead of @Transactional to keep status updates
                    // (markRunning/markSuccess/markFailed) outside the transaction boundary —
                    // they must always commit even if the upsert transaction rolls back.
                    val upsertedCount =
                        transactionalOperator.executeAndAwait {
                            val sourceId = requireNotNull(runningSource.id) { "Event source must be persisted before importing" }
                            eventUpsertService.upsertAndCleanup(result.events, runningSource.venueId, venue.slug, sourceId)
                        }

                    markSuccess(runningSource, upsertedCount, result.etag, result.lastModified)
                    ImportResultResponse(sourceSlug = runningSource.slug, imported = true, eventCount = upsertedCount)
                }
            }
        } catch (e: Exception) {
            val error = e.message ?: "Unknown error during import"
            logger.error(e) { "Import failed for source '${runningSource.slug}': $error" }
            markFailed(runningSource, error)
            ImportResultResponse(sourceSlug = runningSource.slug, imported = false, eventCount = 0, error = error)
        }
    }

    // -- Event source status management --

    private suspend fun markRunning(source: EventSourceEntity): EventSourceEntity =
        saveWithVersionConflictRetry(source) {
            it.copy(
                status = ImportStatus.RUNNING.name,
                lastError = null,
                lastImportAt = Instant.now(clock) // Record when the import started for staleness detection
            )
        }

    private suspend fun markSuccess(
        source: EventSourceEntity,
        eventCount: Int,
        newEtag: String? = source.etag,
        newLastModified: String? = source.lastModified
    ): EventSourceEntity =
        saveWithVersionConflictRetry(source) {
            it.copy(
                status = ImportStatus.SUCCESS.name,
                lastImportAt = Instant.now(clock),
                lastEventCount = eventCount,
                lastError = null,
                etag = newEtag,
                lastModified = newLastModified,
                retryCount = 0
            )
        }

    private suspend fun markFailed(
        source: EventSourceEntity,
        error: String
    ): EventSourceEntity =
        saveWithVersionConflictRetry(source) {
            it.copy(
                status = ImportStatus.FAILED.name,
                lastImportAt = Instant.now(clock),
                lastError = error.take(MAX_ERROR_LENGTH),
                retryCount = it.retryCount + 1
            )
        }

    /**
     * Marks a source as misconfigured — a permanent configuration error that
     * will never self-resolve on retry (e.g. unknown source type, missing importer).
     *
     * Unlike [markFailed], this does NOT increment [EventSourceEntity.retryCount]
     * because retrying is pointless for configuration errors. The scheduler skips
     * MISCONFIGURED sources entirely, so they require manual intervention
     * (fix the config, then call retry to reset to IDLE).
     */
    private suspend fun markMisconfigured(
        source: EventSourceEntity,
        error: String
    ): EventSourceEntity =
        saveWithVersionConflictRetry(source) {
            it.copy(
                status = ImportStatus.MISCONFIGURED.name,
                lastImportAt = Instant.now(clock),
                lastError = error.take(MAX_ERROR_LENGTH)
            )
        }

    /**
     * Saves the [source] entity after applying [mutation], with a single retry on
     * [OptimisticLockingFailureException].
     *
     * An optimistic locking conflict can occur when an external writer (e.g.
     * [ScheduledImportService.resetStuckSources]) modifies the `event_source` row
     * between `markRunning` and `markSuccess`/`markFailed`, making the in-memory
     * `@Version` stale. This is a rare but possible race condition (see ADR-009).
     *
     * On conflict, the entity is re-fetched from the database to obtain the latest
     * version, the [mutation] is re-applied, and the save is retried once. If the
     * retry also fails, the exception propagates — the scheduler will pick up the
     * source on the next tick.
     */
    private suspend fun saveWithVersionConflictRetry(
        source: EventSourceEntity,
        mutation: (EventSourceEntity) -> EventSourceEntity
    ): EventSourceEntity =
        try {
            eventSourceRepository.save(mutation(source))
        } catch (e: OptimisticLockingFailureException) {
            val sourceId = requireNotNull(source.id) { "Cannot retry save for unpersisted event source" }
            logger.warn(e) { "Optimistic locking conflict for source '${source.slug}' (id=$sourceId), re-fetching and retrying" }
            val freshSource =
                eventSourceRepository.findById(sourceId)
                    ?: error("Event source '${source.slug}' (id=$sourceId) disappeared during retry")
            eventSourceRepository.save(mutation(freshSource))
        }

    companion object {
        /** Maximum length for error messages stored in the database. */
        private const val MAX_ERROR_LENGTH = 1000

        /** Default concurrency limit for parallel source imports. */
        internal const val DEFAULT_MAX_CONCURRENCY = 4
    }
}
