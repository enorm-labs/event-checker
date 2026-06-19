package de.norm.events.artist

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive read repository for [ArtistEntity] via R2DBC.
 */
interface ArtistRepository : CoroutineCrudRepository<ArtistEntity, Long> {
    /** Finds all artists with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<ArtistEntity>

    /** Case-insensitive name search with pagination, used by the artist list/search endpoint. */
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable
    ): Flow<ArtistEntity>

    /** Total count matching a case-insensitive name search, for pagination metadata. */
    suspend fun countByNameContainingIgnoreCase(name: String): Long

    /** Finds a single artist by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): ArtistEntity?

    /** Batch-fetches artists by ID — used to resolve event lineups for an event page. */
    fun findByIdIn(ids: Collection<Long>): Flow<ArtistEntity>
}
