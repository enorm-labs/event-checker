package de.norm.events.event

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDate

/**
 * Reactive read repository for [EventEntity] via R2DBC.
 *
 * Dynamic multi-filter search is handled separately by [EventSearchRepository]; the derived
 * methods here cover the fixed-shape queries (slug lookup, today, calendar range).
 */
interface EventRepository : CoroutineCrudRepository<EventEntity, Long> {
    /** Finds a single event by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): EventEntity?

    /** Events on a single calendar [date], ordered by start time — backs the "today" endpoint. */
    fun findByEventDateOrderByStartTime(date: LocalDate): Flow<EventEntity>

    /** Events within an inclusive date range, ordered for calendar display. */
    fun findByEventDateBetweenOrderByEventDateAscStartTimeAsc(
        from: LocalDate,
        to: LocalDate
    ): Flow<EventEntity>
}

/**
 * Reactive read repository for the `event_artist` join table.
 */
interface EventArtistRepository : CoroutineCrudRepository<EventArtistEntity, Long> {
    /** Fetches artist associations for a single event. */
    fun findByEventId(eventId: Long): Flow<EventArtistEntity>

    /** Batch-fetches artist associations for multiple events to avoid N+1 queries. */
    fun findByEventIdIn(eventIds: Collection<Long>): Flow<EventArtistEntity>
}

/**
 * Reactive read repository for the `event_promoter` join table.
 */
interface EventPromoterRepository : CoroutineCrudRepository<EventPromoterEntity, Long> {
    /** Fetches promoter associations for a single event. */
    fun findByEventId(eventId: Long): Flow<EventPromoterEntity>

    /** Batch-fetches promoter associations for multiple events to avoid N+1 queries. */
    fun findByEventIdIn(eventIds: Collection<Long>): Flow<EventPromoterEntity>
}
