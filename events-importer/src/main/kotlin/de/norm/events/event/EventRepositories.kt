package de.norm.events.event

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive repository for [EventEntity] persistence via R2DBC.
 */
interface EventRepository : CoroutineCrudRepository<EventEntity, Long> {
    suspend fun findBySourceId(sourceId: String): EventEntity?

    /** Finds all events with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<EventEntity>
}

/**
 * Repository for the `event_artist` join table.
 */
interface EventArtistRepository : CoroutineCrudRepository<EventArtistEntity, Long> {
    fun findByEventId(eventId: Long): Flow<EventArtistEntity>

    /** Batch-fetches artist associations for multiple events to avoid N+1 queries. */
    fun findByEventIdIn(eventIds: Collection<Long>): Flow<EventArtistEntity>

    suspend fun deleteByEventId(eventId: Long)
}

/**
 * Repository for the `event_promoter` join table.
 */
interface EventPromoterRepository : CoroutineCrudRepository<EventPromoterEntity, Long> {
    fun findByEventId(eventId: Long): Flow<EventPromoterEntity>

    /** Batch-fetches promoter associations for multiple events to avoid N+1 queries. */
    fun findByEventIdIn(eventIds: Collection<Long>): Flow<EventPromoterEntity>

    suspend fun deleteByEventId(eventId: Long)
}
