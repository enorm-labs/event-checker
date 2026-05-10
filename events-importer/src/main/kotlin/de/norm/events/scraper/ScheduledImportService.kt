package de.norm.events.scraper

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

/**
 * Periodic scheduler for event imports.
 *
 * Runs a tick every 60 seconds to find event sources that are due and delegates
 * each to [EventImportService.importFromSource]. This is a thin orchestration layer
 * on top of the existing import infrastructure — it adds:
 *
 * - **Per-source scheduling**: each source has its own `importIntervalMinutes`.
 * - **Retry with exponential backoff**: failed sources are retried up to `maxRetries`
 *   times, with the interval doubling on each consecutive failure.
 * - **Staleness detection**: sources stuck in RUNNING for >30 min are reset to FAILED.
 * - **Overlap prevention**: sources with status = RUNNING are skipped.
 * - **Misconfiguration detection**: sources with status = MISCONFIGURED are skipped entirely
 *   (they have a permanent config error that requires manual intervention).
 *
 * Scheduling can be disabled via `app.scheduling.enabled=false` (e.g. in tests).
 *
 * @see EventSourceEntity for scheduling fields
 * @see EventImportService for the import pipeline
 */
@Service
@ConditionalOnProperty(name = ["app.scheduling.enabled"], havingValue = "true", matchIfMissing = true)
class ScheduledImportService(
    private val eventSourceRepository: EventSourceRepository,
    private val eventImportService: EventImportService,
    private val clock: Clock = Clock.systemUTC(),
    /** Configurable staleness timeout — sources stuck in RUNNING longer than this are reset to FAILED. */
    @Value($$"${app.scheduling.staleness-timeout:30m}")
    private val stalenessTimeout: Duration = DEFAULT_STALENESS_TIMEOUT
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Main scheduler tick — runs every 60 seconds.
     *
     * Spring Boot 4 (Spring Framework 7) natively supports Kotlin `suspend` functions
     * in `@Scheduled` methods, so no `runBlocking` bridge is needed. Spring dispatches
     * the coroutine on an appropriate scheduler, keeping the Netty event loop free.
     *
     * Note: `$$"..."` uses Kotlin multi-dollar raw string to pass Spring property placeholder
     * without interpolation — `$$` raises the interpolation threshold so `${...}` is literal.
     */
    @Scheduled(fixedDelayString = $$"${app.scheduling.tick-interval:60000}")
    suspend fun tick() {
        resetStuckSources()
        importDueSources()
    }

    /**
     * Finds and imports all sources that are due based on their individual schedule.
     *
     * A source is due when its `lastImportAt` + `importIntervalMinutes` is in the past.
     * Failed sources use exponential backoff: `importIntervalMinutes × 2^retryCount`.
     */
    private suspend fun importDueSources() {
        // Capture a single timestamp for the entire tick to ensure consistent
        // due-date evaluation across all sources (avoids clock drift within a tick).
        val now = Instant.now(clock)
        val candidates = eventSourceRepository.findDueForImport(now).toList()

        // Filter to sources that are actually due based on their individual interval + backoff
        val dueSources = candidates.filter { isDue(it, now) }

        if (dueSources.isEmpty()) return

        logger.info { "Scheduler tick: ${dueSources.size} source(s) due for import" }

        // Concurrent execution is safe — per-host politeness is enforced by PerHostThrottlingFilter,
        // the artist cache is local to each importFromSource call, and each source runs in its own transaction.
        eventImportService.importConcurrently(dueSources)
    }

    /**
     * Checks if a source is due for import based on its schedule and retry backoff.
     *
     * A source is due when:
     * - It has never been imported, OR
     * - Enough time has passed since the last import to satisfy the interval.
     *
     * For failed sources, the interval is multiplied by `2^retryCount` to provide
     * exponential backoff (e.g. 1440min → 2880min → 5760min).
     *
     * @param now the reference timestamp for the current tick (captured once per tick
     *   for consistency across all sources).
     */
    internal fun isDue(
        source: EventSourceEntity,
        now: Instant
    ): Boolean {
        // Sources with no import history or in IDLE status (e.g. after manual retry) are always due.
        // IDLE check allows retry() to trigger immediate pickup without clearing lastImportAt,
        // preserving the historical record of when the last import ran.
        val lastImport = source.lastImportAt
        if (lastImport == null || source.status == ImportStatus.IDLE.name) return true

        val baseInterval = Duration.ofMinutes(source.importIntervalMinutes.toLong())
        val effectiveInterval =
            if (source.status == ImportStatus.FAILED.name && source.retryCount > 0) {
                // Exponential backoff: double the interval for each retry (2^retryCount)
                val backoffMultiplier = 2.0.pow(source.retryCount.coerceAtMost(MAX_BACKOFF_EXPONENT)).toLong()
                baseInterval.multipliedBy(backoffMultiplier)
            } else {
                baseInterval
            }

        return now.isAfter(lastImport.plus(effectiveInterval))
    }

    /**
     * Resets sources stuck in RUNNING status to FAILED.
     *
     * This guards against imports that never completed (e.g. due to application crash
     * or network timeout without proper error handling). Sources stuck for longer than
     * [stalenessTimeout] (configurable via `app.scheduling.staleness-timeout`, default: 30m)
     * are considered stale.
     */
    private suspend fun resetStuckSources() {
        val stalenessCutoff = Instant.now(clock).minus(stalenessTimeout)
        val stuckSources = eventSourceRepository.findStuckSources(stalenessCutoff).toList()

        for (source in stuckSources) {
            logger.warn { "Resetting stuck source '${source.slug}' from RUNNING to FAILED (last import: ${source.lastImportAt})" }
            try {
                eventSourceRepository.save(
                    source.copy(
                        status = ImportStatus.FAILED.name,
                        lastError = "Import timed out (stuck in RUNNING for >${stalenessTimeout.toMinutes()} minutes)",
                        retryCount = source.retryCount + 1
                    )
                )
            } catch (e: OptimisticLockingFailureException) {
                // The source was concurrently updated (e.g. the import just finished),
                // so it's no longer stuck — safe to skip. The next tick will re-evaluate.
                logger.info(e) { "Skipping stuck-source reset for '${source.slug}': version conflict indicates concurrent update" }
            }
        }
    }

    companion object {
        /**
         * Maximum exponent for backoff to prevent overflow.
         * 2^6 = 64x multiplier → a daily job would max out at ~64 days between retries.
         */
        private const val MAX_BACKOFF_EXPONENT = 6

        /** Default staleness timeout: sources stuck in RUNNING for longer than this are reset to FAILED. */
        private val DEFAULT_STALENESS_TIMEOUT: Duration = Duration.ofMinutes(30)
    }
}
