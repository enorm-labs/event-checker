package de.norm.events.promoter

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive read repository for [PromoterEntity] via R2DBC.
 */
interface PromoterRepository : CoroutineCrudRepository<PromoterEntity, Long> {
    /** Finds all promoters with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<PromoterEntity>

    /** Case-insensitive name search with pagination, used by the promoter list endpoint. */
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable
    ): Flow<PromoterEntity>

    /** Total count matching a case-insensitive name search, for pagination metadata. */
    suspend fun countByNameContainingIgnoreCase(name: String): Long

    /** Finds a single promoter by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): PromoterEntity?

    /** Batch-fetches promoters by ID — used to resolve embedded promoters for an event page. */
    fun findByIdIn(ids: Collection<Long>): Flow<PromoterEntity>
}
