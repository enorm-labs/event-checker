package de.norm.events.genretag

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive repository for [GenreTagEntity] persistence via R2DBC.
 */
interface GenreTagRepository : CoroutineCrudRepository<GenreTagEntity, Long> {
    /** Finds all genre tags with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<GenreTagEntity>

    /** Finds a single genre tag by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): GenreTagEntity?

    /** Batch-fetches genre tags by their slugs. Used by the import pipeline to avoid N+1 queries. */
    fun findBySlugIn(slugs: Collection<String>): Flow<GenreTagEntity>
}

/**
 * Repository for the `event_genre_tag` join table.
 */
interface EventGenreTagRepository : CoroutineCrudRepository<EventGenreTagEntity, Long> {
    fun findByEventId(eventId: Long): Flow<EventGenreTagEntity>

    /** Batch-fetches genre tag associations for multiple events to avoid N+1 queries. */
    fun findByEventIdIn(eventIds: Collection<Long>): Flow<EventGenreTagEntity>

    suspend fun deleteByEventId(eventId: Long)
}
