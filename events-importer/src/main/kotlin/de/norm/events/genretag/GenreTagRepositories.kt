package de.norm.events.genretag

import de.norm.events.EVENTS_SCHEMA
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GenreTagRepository : CoroutineCrudRepository<GenreTagEntity, Long> {
    /** Finds all genre tags with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<GenreTagEntity>

    /** Finds a single genre tag by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): GenreTagEntity?

    /** Batch-fetches genre tags by their slugs. Used by the import pipeline to avoid N+1 queries. */
    fun findBySlugIn(slugs: Collection<String>): Flow<GenreTagEntity>

    /**
     * Inserts a genre tag only if its [slug] is not already taken, returning the number of rows
     * inserted (`1` if created, `0` if it already existed).
     *
     * `ON CONFLICT DO NOTHING` makes a duplicate slug a no-op instead of raising, so a concurrent
     * import inserting the same tag first does not abort the caller's transaction.
     * `created_at`/`updated_at` fall back to their `DEFAULT now()`.
     */
    @Modifying
    @Query(
        "INSERT INTO $EVENTS_SCHEMA.genre_tag (name, slug, family) VALUES (:name, :slug, :family) ON CONFLICT (slug) DO NOTHING"
    )
    suspend fun insertIfAbsent(
        name: String,
        slug: String,
        family: String?
    ): Int

    /**
     * Sets the family of the tag with this [id], returning the number of rows changed. A derived
     * `updateBy*` is not available in R2DBC, hence the raw statement.
     */
    @Modifying
    @Query("UPDATE $EVENTS_SCHEMA.genre_tag SET family = :family, updated_at = now() WHERE id = :id")
    suspend fun updateFamily(
        id: Long,
        family: String?
    ): Int
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
