package de.norm.events.scraper

import de.norm.events.event.EventEntity
import de.norm.events.event.EventRepository
import de.norm.events.slug.SlugGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

/**
 * Handles the persistence pipeline for scraped events: deduplication, upsert, and
 * stale event cleanup.
 *
 * Association management (artist/promoter resolution and join-table syncing) is
 * delegated to [AssociationSyncService] to keep this service focused on event-level
 * persistence concerns. Called within a transactional boundary managed by the caller —
 * it does not manage its own transactions.
 */
@Service
class EventUpsertService(
    private val eventRepository: EventRepository,
    private val associationSyncService: AssociationSyncService,
    /** Injected clock for deterministic time in tests. Defaults to system UTC clock in production. */
    private val clock: Clock = Clock.systemUTC()
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Deduplicates, upserts, and cleans up stale events for a single event source.
     *
     * This method should be called within a transactional boundary so partial failures
     * roll back cleanly. The full pipeline:
     * 1. Drop scraped events dated before today (see [dropPastEvents]).
     * 2. Deduplicate scraped events by generated slug (date + title).
     * 3. Upsert events into the database (insert new, update existing by `sourceId`).
     * 4. Resolve and sync artist/promoter associations (delegated to [AssociationSyncService]).
     * 5. Remove stale future events no longer listed on the source website.
     *
     * @param scrapedEvents the raw events from the scraper (may contain duplicates).
     * @param venueId the database ID of the venue these events belong to.
     * @param venueSlug the URL-friendly slug of the venue, included in event slugs for cross-venue uniqueness.
     * @param eventSourceId the database ID of the [EventSourceEntity] that owns these events.
     * @return the number of events upserted after deduplication.
     */
    suspend fun upsertAndCleanup(
        scrapedEvents: List<ScrapedEvent>,
        venueId: Long,
        venueSlug: String,
        eventSourceId: Long
    ): Int {
        val upcomingEvents = dropPastEvents(scrapedEvents, eventSourceId)
        val uniqueEvents = deduplicateScrapedEvents(upcomingEvents)
        val upserted = upsertEvents(uniqueEvents, venueId, venueSlug, eventSourceId)
        removeStaleEvents(uniqueEvents, eventSourceId)
        return upserted.size
    }

    /**
     * Drops scraped events dated before today, keeping today onward.
     *
     * Calendar-style sources publish the venue's whole standing programme — including
     * shows that have already happened (a widget returning the full calendar, or a CMS
     * page that leaves recently-passed nights listed). Because [removeStaleEvents] never
     * prunes past-dated rows (it preserves them for historical records), re-importing such
     * a source would otherwise resurrect stale events on every run. Filtering here — the
     * single funnel every source flows through — stops that universally.
     *
     * This is the ingestion-side dual of [removeStaleEvents]'s cutoff: that method keeps
     * the future as the live window on cleanup; this one does the same on intake. Same-day
     * events are kept (the show may still be running), matching the `tomorrow` lower bound
     * used for cleanup. Existing past-dated rows are untouched — they are simply not
     * re-upserted, so nothing is lost for a source scraped regularly (events age into the
     * past only after they were first imported while still upcoming).
     *
     * @param scrapedEvents the raw events from the scraper.
     * @param eventSourceId the database ID of the [EventSourceEntity] that owns these events, used for logging.
     * @return the scraped events dated today or later.
     */
    private fun dropPastEvents(
        scrapedEvents: List<ScrapedEvent>,
        eventSourceId: Long
    ): List<ScrapedEvent> {
        val today = LocalDate.now(clock)
        val (upcoming, past) = scrapedEvents.partition { !it.eventDate.isBefore(today) }
        if (past.isNotEmpty()) {
            logger.info { "Dropped ${past.size} past event(s) from event source $eventSourceId" }
        }
        return upcoming
    }

    /**
     * Upserts pre-deduplicated scraped events into the database.
     *
     * For each event, checks if an event with the same `sourceId` already
     * exists. If so, compares the built entity against the existing row and
     * only saves it when business-relevant fields have changed — unchanged
     * events are skipped to avoid unnecessary UPDATE statements and inflated
     * `updated_at` timestamps. New events are always inserted. Artist and
     * promoter associations are resolved and synced by [AssociationSyncService].
     *
     * @return the full list of event entities (saved + unchanged).
     */
    private suspend fun upsertEvents(
        scrapedEvents: List<ScrapedEvent>,
        venueId: Long,
        venueSlug: String,
        eventSourceId: Long
    ): List<EventEntity> {
        val existingBySourceId =
            eventRepository
                .findBySourceIdIn(scrapedEvents.map { it.sourceId })
                .toList()
                .associateBy { it.sourceId }

        // 1. Build all event entities in memory, skip unchanged ones, and bulk-save only changes.
        //    This avoids unnecessary UPDATE statements and inflated updated_at timestamps for events
        //    where the scraped data hasn't changed since the last import.
        val entities =
            scrapedEvents.map { scraped ->
                scraped.toEventEntity(venueId, venueSlug, eventSourceId, existingBySourceId[scraped.sourceId])
            }
        val (changed, unchanged) = partitionByChanged(entities, existingBySourceId)
        val savedEvents =
            if (changed.isNotEmpty()) {
                eventRepository.saveAll(changed).toList() + unchanged
            } else {
                unchanged
            }

        // 2. Resolve artists/promoters and sync associations (delegated to AssociationSyncService)
        associationSyncService.resolveAndSyncAssociations(savedEvents, scrapedEvents)

        // 3. Log upsert results (only for changed/new events — unchanged ones are already logged in partitionByChanged)
        changed.forEach { saved ->
            val action = if (existingBySourceId.containsKey(saved.sourceId)) "Updated" else "Created"
            logger.debug { "$action event '${saved.title}' (sourceId=${saved.sourceId}, id=${saved.id})" }
        }
        return savedEvents
    }

    /**
     * Removes duplicate events from the scraped list.
     *
     * Duplicates are identified by the date + title combination (e.g. `2026-05-17-open-decks`).
     * Within a single import all events belong to the same venue, so the venue slug is
     * intentionally omitted from the dedup key. When a source website accidentally lists
     * the same event twice with different URL paths, they share the same date + title but
     * have different [ScrapedEvent.sourceId]s. Keeping only the first occurrence prevents
     * unique constraint violations on `event.slug` during insert.
     */
    private fun deduplicateScrapedEvents(events: List<ScrapedEvent>): List<ScrapedEvent> {
        val seen = mutableSetOf<String>()
        return events.filter { event ->
            val slug = SlugGenerator.slugify("${event.eventDate}-${event.title}")
            val isNew = seen.add(slug)
            if (!isNew) {
                logger.warn { "Skipping duplicate event '${event.title}' on ${event.eventDate} (sourceId=${event.sourceId})" }
            }
            isNew
        }
    }

    /**
     * Removes future events that were previously imported from this source but are
     * no longer listed on the venue's website (e.g. cancelled or removed events).
     *
     * Only events from tomorrow up to the latest scraped date are considered — this
     * prevents deleting events on pages we didn't fetch (e.g. when only page 1 of a
     * paginated listing is scraped). Past events are always preserved for historical
     * records regardless of whether they still appear on the source website.
     *
     * **Why tomorrow, not today?** Many venue websites naturally stop listing events
     * once the day begins (showing only "upcoming" events from tomorrow onward). Using
     * `today` as the lower bound would incorrectly delete same-day events that are
     * actually happening but simply no longer appear in the listing. Starting from
     * tomorrow avoids these false deletions. The trade-off is that a genuinely
     * cancelled today-event stays in the DB for at most a few hours until it becomes
     * a past event — which we preserve for historical records anyway.
     *
     * @param scrapedEvents the events from the current scrape (used to determine
     *   the date range and the set of known sourceIds).
     * @param eventSourceId the database ID of the [EventSourceEntity] that owns these events,
     *   used to query by FK instead of text-pattern matching.
     */
    private suspend fun removeStaleEvents(
        scrapedEvents: List<ScrapedEvent>,
        eventSourceId: Long
    ) {
        if (scrapedEvents.isEmpty()) return

        val tomorrow = LocalDate.now(clock).plusDays(1)
        val maxScrapedDate = scrapedEvents.maxOf { it.eventDate }
        val scrapedSourceIds = scrapedEvents.map { it.sourceId }.toSet()

        // Find all events from this source within the cleanup window via FK.
        // Starts from tomorrow to avoid deleting same-day events that venues
        // may have simply stopped listing — see KDoc for rationale.
        val existingEvents =
            eventRepository
                .findByEventSourceIdAndEventDateBetween(
                    eventSourceId = eventSourceId,
                    fromDate = tomorrow,
                    toDate = maxScrapedDate
                ).toList()

        val staleEvents = existingEvents.filter { it.sourceId !in scrapedSourceIds }

        if (staleEvents.isNotEmpty()) {
            val staleIds = staleEvents.mapNotNull { it.id }
            eventRepository.deleteByIdIn(staleIds)
            staleEvents.forEach { event ->
                logger.info { "Removed stale event '${event.title}' on ${event.eventDate} (sourceId=${event.sourceId}, id=${event.id})" }
            }
            logger.info { "Removed ${staleEvents.size} stale event(s) no longer listed on event source $eventSourceId" }
        }
    }

    /**
     * Partitions built entities into those that actually changed (or are new) vs. those identical
     * to their existing database row. Only changed/new entities need to be saved, avoiding
     * unnecessary UPDATE statements and inflated `updated_at` timestamps.
     *
     * @return a pair of (changed/new entities, unchanged entities).
     */
    private fun partitionByChanged(
        entities: List<EventEntity>,
        existingBySourceId: Map<String, EventEntity>
    ): Pair<List<EventEntity>, List<EventEntity>> {
        val changed = mutableListOf<EventEntity>()
        val unchanged = mutableListOf<EventEntity>()

        for (entity in entities) {
            val existing = existingBySourceId[entity.sourceId]
            if (existing == null || !entity.contentEquals(existing)) {
                changed.add(entity)
            } else {
                unchanged.add(entity)
                logger.debug { "Skipping unchanged event '${entity.title}' (sourceId=${entity.sourceId})" }
            }
        }

        if (unchanged.isNotEmpty()) {
            logger.info { "Skipped ${unchanged.size} unchanged event(s), saving ${changed.size} changed/new event(s)" }
        }

        return changed to unchanged
    }

    /**
     * Checks whether this entity has the same business-relevant content as [other].
     *
     * Normalizes audit fields (`id`, `createdAt`, `updatedAt`) before comparing via
     * the data class `equals()`, so only actual data changes are detected. Because
     * `equals()` covers all constructor properties, newly added fields are automatically
     * included without manual maintenance.
     *
     * This extension lives in the scraper module (not on [EventEntity] itself) because
     * content-based change detection is a scraper concern. Overriding `equals()`/`hashCode()`
     * on the entity would break Spring Data R2DBC identity semantics and collection behavior.
     */
    private fun EventEntity.contentEquals(other: EventEntity): Boolean = copy(id = other.id, createdAt = other.createdAt, updatedAt = other.updatedAt) == other
}
