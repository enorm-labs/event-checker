package de.norm.events.genretag

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive read repository for [GenreTagEntity] via R2DBC.
 */
interface GenreTagRepository : CoroutineCrudRepository<GenreTagEntity, Long> {
    /** Lists all genre tags alphabetically — used to populate the frontend genre filter dropdown. */
    fun findAllByOrderByName(): Flow<GenreTagEntity>
}

/**
 * Reactive read repository for the `event_genre_tag` join table.
 */
interface EventGenreTagRepository : CoroutineCrudRepository<EventGenreTagEntity, Long> {
    /** Batch-fetches genre tag associations for multiple events to avoid N+1 queries. */
    fun findByEventIdIn(eventIds: Collection<Long>): Flow<EventGenreTagEntity>

    /** Fetches genre tag associations for a single event. */
    fun findByEventId(eventId: Long): Flow<EventGenreTagEntity>
}
