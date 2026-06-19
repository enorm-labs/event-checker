package de.norm.events.venue

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive read repository for [VenueEntity] via R2DBC.
 */
interface VenueRepository : CoroutineCrudRepository<VenueEntity, Long> {
    /** Finds all venues with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<VenueEntity>

    /** Case-insensitive name search with pagination, used by the venue list endpoint. */
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable
    ): Flow<VenueEntity>

    /** Total count matching a case-insensitive name search, for pagination metadata. */
    suspend fun countByNameContainingIgnoreCase(name: String): Long

    /** Finds a single venue by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): VenueEntity?

    /** Batch-fetches venues by ID — used to resolve embedded venue summaries for an event page. */
    fun findByIdIn(ids: Collection<Long>): Flow<VenueEntity>
}
