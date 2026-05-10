package de.norm.events.scraper

import de.norm.events.artist.ArtistEntity
import de.norm.events.artist.ArtistRepository
import de.norm.events.event.EventArtistEntity
import de.norm.events.event.EventArtistRepository
import de.norm.events.event.EventEntity
import de.norm.events.event.EventRepository
import de.norm.events.slug.SlugGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

/**
 * Handles the persistence pipeline for scraped events: deduplication, upsert, artist
 * resolution, association syncing, and stale event cleanup.
 *
 * Extracted from [EventImportService] to separate persistence concerns from import
 * orchestration. This service is called within a transactional boundary managed by the
 * caller — it does not manage its own transactions.
 */
@Service
class EventUpsertService(
    private val eventRepository: EventRepository,
    private val eventArtistRepository: EventArtistRepository,
    private val artistRepository: ArtistRepository,
    /** Injected clock for deterministic time in tests. Defaults to system UTC clock in production. */
    private val clock: Clock = Clock.systemUTC()
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Deduplicates, upserts, and cleans up stale events for a single event source.
     *
     * This method should be called within a transactional boundary so partial failures
     * roll back cleanly. The full pipeline:
     * 1. Deduplicate scraped events by generated slug (date + title).
     * 2. Upsert events into the database (insert new, update existing by `sourceId`).
     * 3. Auto-create unknown artists with generated slugs.
     * 4. Sync artist associations (diff-based insert/update/delete).
     * 5. Remove stale future events no longer listed on the source website.
     *
     * @param scrapedEvents the raw events from the scraper (may contain duplicates).
     * @param venueId the database ID of the venue these events belong to.
     * @param eventSourceId the database ID of the [EventSourceEntity] that owns these events.
     * @return the number of events upserted after deduplication.
     */
    suspend fun upsertAndCleanup(
        scrapedEvents: List<ScrapedEvent>,
        venueId: Long,
        eventSourceId: Long
    ): Int {
        val uniqueEvents = deduplicateScrapedEvents(scrapedEvents)
        val upserted = upsertEvents(uniqueEvents, venueId, eventSourceId)
        removeStaleEvents(uniqueEvents, eventSourceId)
        return upserted.size
    }

    /**
     * Upserts pre-deduplicated scraped events into the database.
     *
     * For each event, checks if an event with the same `sourceId` already
     * exists. If so, compares the built entity against the existing row and
     * only saves it when business-relevant fields have changed — unchanged
     * events are skipped to avoid unnecessary UPDATE statements and inflated
     * `updated_at` timestamps. New events are always inserted. Artist associations
     * are resolved by name (auto-creating unknown artists with generated slugs).
     *
     * @return the full list of event entities (saved + unchanged).
     */
    private suspend fun upsertEvents(
        scrapedEvents: List<ScrapedEvent>,
        venueId: Long,
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
                scraped.toEventEntity(venueId, eventSourceId, existingBySourceId[scraped.sourceId])
            }
        val (changed, unchanged) = partitionByChanged(entities, existingBySourceId)
        val savedEvents =
            if (changed.isNotEmpty()) {
                eventRepository.saveAll(changed).toList() + unchanged
            } else {
                unchanged
            }

        // 2. Resolve artists and sync associations (diff-based: insert/update/delete only changes)
        val artistCache = resolveAllArtists(scrapedEvents)
        syncArtistAssociations(savedEvents, scrapedEvents, artistCache)

        // 3. Log upsert results (only for changed/new events — unchanged ones are already logged in partitionByChanged)
        changed.forEach { saved ->
            val action = if (existingBySourceId.containsKey(saved.sourceId)) "Updated" else "Created"
            logger.debug { "$action event '${saved.title}' (sourceId=${saved.sourceId}, id=${saved.id})" }
        }
        return savedEvents
    }

    /**
     * Batch-fetches all known artists by slug and auto-creates any unknown artists.
     *
     * @return a cache mapping artist slug → persisted [ArtistEntity], covering all
     *   artists referenced by the scraped events.
     */
    private suspend fun resolveAllArtists(scrapedEvents: List<ScrapedEvent>): Map<String, ArtistEntity> {
        val allArtistSlugs = scrapedEvents.flatMap { it.artists }.map { SlugGenerator.slugify(it.name) }.toSet()
        val artistCache =
            artistRepository
                .findBySlugIn(allArtistSlugs)
                .toList()
                .associateBy { it.slug }
                .toMutableMap()

        // Auto-create only the artists not already in the database
        scrapedEvents
            .flatMap { it.artists }
            .distinctBy { SlugGenerator.slugify(it.name) }
            .forEach { resolveOrCreateArtist(it.name, artistCache) }

        return artistCache
    }

    /**
     * Synchronizes artist associations for the given saved events using a diff strategy.
     *
     * Instead of deleting and re-creating all associations on every import (which wastes
     * auto-increment IDs and causes unnecessary write churn), this method compares
     * existing associations against the desired state and only:
     * - **Inserts** truly new associations (artist added to event).
     * - **Updates** associations where role or billing order changed.
     * - **Deletes** associations for artists no longer linked to the event.
     * - **Skips** associations that are already correct (no-op).
     *
     * Associations are matched by the composite key `(eventId, artistId)`.
     */
    private suspend fun syncArtistAssociations(
        savedEvents: List<EventEntity>,
        scrapedEvents: List<ScrapedEvent>,
        artistCache: Map<String, ArtistEntity>
    ) {
        val savedEventIds = savedEvents.mapNotNull { it.id }
        if (savedEventIds.isEmpty()) return

        // Batch-fetch all existing associations for the upserted events
        val existingByEventId =
            eventArtistRepository
                .findByEventIdIn(savedEventIds)
                .toList()
                .groupBy { it.eventId }

        // Build the desired associations from scraped data
        val artistsBySourceId = scrapedEvents.associate { it.sourceId to it.artists }
        val toInsert = mutableListOf<EventArtistEntity>()
        val toUpdate = mutableListOf<EventArtistEntity>()
        val toDeleteIds = mutableListOf<Long>()

        for (saved in savedEvents) {
            val eventId = requireNotNull(saved.id) { "Saved event must have an ID" }
            val existing = existingByEventId[eventId].orEmpty()
            val existingByArtistId = existing.associateBy { it.artistId }

            val desiredArtists = artistsBySourceId[saved.sourceId].orEmpty()
            val desiredArtistIds = mutableSetOf<Long>()

            for ((index, scrapedArtist) in desiredArtists.withIndex()) {
                val slug = SlugGenerator.slugify(scrapedArtist.name)
                val artistId = requireNotNull(artistCache[slug]?.id) { "Artist '$slug' must be resolved before syncing associations" }
                desiredArtistIds.add(artistId)

                val desired = scrapedArtist.toEventArtistEntity(eventId, artistId, billingOrder = index)
                val current = existingByArtistId[artistId]
                if (current == null) {
                    // New association — insert
                    toInsert.add(desired)
                } else if (current.role != desired.role || current.billingOrder != desired.billingOrder) {
                    // Association exists but role or billing order changed — update in place
                    toUpdate.add(current.copy(role = desired.role, billingOrder = desired.billingOrder))
                }
                // else: association is already correct — no-op
            }

            // Associations for artists no longer linked to this event — delete
            existing
                .filter { it.artistId !in desiredArtistIds }
                .mapNotNull { it.id }
                .let { toDeleteIds.addAll(it) }
        }

        // Apply all changes in bulk
        if (toDeleteIds.isNotEmpty()) {
            eventArtistRepository.deleteAllById(toDeleteIds)
        }
        if (toUpdate.isNotEmpty()) {
            eventArtistRepository.saveAll(toUpdate).toList()
        }
        if (toInsert.isNotEmpty()) {
            eventArtistRepository.saveAll(toInsert).toList()
        }
    }

    /**
     * Removes duplicate events from the scraped list.
     *
     * Duplicates are identified by the slug that would be generated from their
     * date and title (e.g. `2026-05-17-open-decks`). When a source website
     * accidentally lists the same event twice with different URL paths, they
     * share the same date + title but have different [ScrapedEvent.sourceId]s.
     * Keeping only the first occurrence prevents unique constraint violations
     * on `event.slug` during insert.
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
            if (existing == null || hasChanges(entity, existing)) {
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
     * Compares a newly built entity against the existing database row to detect meaningful changes.
     *
     * Delegates to [EventEntity.contentEquals], which normalizes audit fields before comparing.
     * See that extension function's KDoc for the rationale on why this lives in the scraper module.
     */
    private fun hasChanges(
        new: EventEntity,
        existing: EventEntity
    ): Boolean = !new.contentEquals(existing)

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

    /**
     * Resolves an artist by slug (derived from name) from the [artistCache],
     * or auto-creates a new artist entity if no match is found. Newly created
     * artists are added to the cache so subsequent events in the same batch
     * can reuse them without additional database queries.
     *
     * Handles concurrent artist creation gracefully: if another import creates
     * the same artist between our cache check and save, the unique constraint
     * violation on `artist.slug` is caught and we fall back to a lookup.
     */
    private suspend fun resolveOrCreateArtist(
        name: String,
        artistCache: MutableMap<String, ArtistEntity>
    ): ArtistEntity {
        val slug = SlugGenerator.slugify(name)
        artistCache[slug]?.let { return it }

        val created =
            try {
                val saved = artistRepository.save(ArtistEntity(name = name, slug = slug))
                logger.info { "Auto-created artist '${saved.name}' (slug=$slug, id=${saved.id})" }
                saved
            } catch (_: DataIntegrityViolationException) {
                // Another concurrent import created this artist — fetch it instead of failing
                logger.debug { "Artist slug '$slug' already exists (concurrent creation), falling back to lookup" }
                artistRepository.findBySlug(slug)
                    ?: throw IllegalStateException("Artist with slug '$slug' disappeared after conflict")
            }
        artistCache[slug] = created
        return created
    }
}
