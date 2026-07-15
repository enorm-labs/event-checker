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

    /** Exact-match filter on the district (Bezirk) slug with pagination. */
    fun findByDistrict(
        district: String,
        pageable: Pageable
    ): Flow<VenueEntity>

    /** Total count of venues in a district, for pagination metadata. */
    suspend fun countByDistrict(district: String): Long

    /** Combined case-insensitive name search and exact district filter with pagination. */
    fun findByNameContainingIgnoreCaseAndDistrict(
        name: String,
        district: String,
        pageable: Pageable
    ): Flow<VenueEntity>

    /** Total count matching both a name search and a district filter, for pagination metadata. */
    suspend fun countByNameContainingIgnoreCaseAndDistrict(
        name: String,
        district: String
    ): Long

    /** Finds a single venue by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): VenueEntity?

    /** Batch-fetches venues by ID — used to resolve embedded venue summaries for an event page. */
    fun findByIdIn(ids: Collection<Long>): Flow<VenueEntity>
}
