package de.norm.events.scraper

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.Instant

/**
 * Reactive repository for [EventSourceEntity] persistence via R2DBC.
 */
interface EventSourceRepository : CoroutineCrudRepository<EventSourceEntity, Long> {
    /** Finds an event source by its unique slug (used for API dispatch). */
    suspend fun findBySlug(slug: String): EventSourceEntity?

    /** Returns all event sources with pagination and sorting support. */
    fun findAllBy(pageable: Pageable): Flow<EventSourceEntity>

    /** Returns all enabled event sources for batch importing. */
    fun findByEnabledTrue(): Flow<EventSourceEntity>

    /**
     * Finds enabled sources that are candidates for import.
     *
     * This is the first phase of a two-phase filtering strategy:
     * 1. **SQL (coarse)**: filters out sources that are definitely not due — disabled,
     *    already running, or exhausted retries. The `last_import_at < :now` clause is
     *    intentionally broad (always true for past timestamps) to keep candidates that
     *    need per-source interval evaluation.
     * 2. **Kotlin (precise)**: [ScheduledImportService.isDue] applies per-source interval
     *    and exponential backoff logic that cannot be expressed in a single SQL query
     *    (each source has its own `importIntervalMinutes` and `retryCount`).
     *
     * Sources with status = 'RUNNING' are excluded to prevent overlapping imports.
     * Sources with status = 'MISCONFIGURED' are excluded because they need manual intervention.
     * Failed sources that have exhausted their retry budget (`retry_count >= max_retries`)
     * are also excluded to avoid fetching rows that will always be skipped.
     * Raw SQL is required because R2DBC does not support derived queries with
     * date arithmetic (see ADR-002).
     *
     * @param now the current timestamp, used as a coarse upper bound on `last_import_at`.
     */
    @Query(
        """
        SELECT * FROM events.event_source
        WHERE enabled = true
          AND status NOT IN ('${ImportStatus.S_RUNNING}', '${ImportStatus.S_MISCONFIGURED}')
          AND (status != '${ImportStatus.S_FAILED}' OR retry_count < max_retries)
          AND (
              last_import_at IS NULL
              OR last_import_at < :now
          )
        """
    )
    fun findDueForImport(now: Instant): Flow<EventSourceEntity>

    /**
     * Finds sources stuck in RUNNING status for longer than the staleness timeout.
     *
     * These are reset to FAILED by the scheduler to prevent permanently stuck imports
     * (e.g. due to an application crash during import).
     */
    @Query(
        """
        SELECT * FROM events.event_source
        WHERE status = '${ImportStatus.S_RUNNING}'
          AND last_import_at IS NOT NULL
          AND last_import_at < :stalenessCutoff
        """
    )
    fun findStuckSources(stalenessCutoff: Instant): Flow<EventSourceEntity>

    /**
     * Bulk-resets all enabled, failed or misconfigured event sources to IDLE for retry.
     *
     * Uses a single UPDATE statement instead of fetch-modify-save per source
     * for better performance when many sources need to be retried.
     * Preserves `last_import_at` as a historical record — [ScheduledImportService.isDue]
     * treats IDLE sources as always-due regardless of when they last ran.
     * Also resets MISCONFIGURED sources so they can be retried after fixing their configuration.
     *
     * Note: This bypasses `@Version` optimistic locking but increments `version` to prevent
     * stale read-modify-write cycles from silently succeeding. Without this increment, a
     * concurrent `findBySlug` + `save` that loaded the entity before this bulk update would
     * still see a matching `version` and overwrite the reset.
     *
     * @return the number of rows updated.
     */
    @Modifying
    @Query(
        """
        UPDATE events.event_source
        SET status = '${ImportStatus.S_IDLE}', retry_count = 0, last_error = NULL, version = version + 1
        WHERE enabled = true AND status IN ('${ImportStatus.S_FAILED}', '${ImportStatus.S_MISCONFIGURED}')
        """
    )
    suspend fun resetAllFailedToIdle(): Int
}
