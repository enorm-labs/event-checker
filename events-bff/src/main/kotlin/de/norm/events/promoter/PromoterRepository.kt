package de.norm.events.promoter

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive read repository for [PromoterEntity] via R2DBC.
 */
interface PromoterRepository : CoroutineCrudRepository<PromoterEntity, Long> {
    /** Finds a single promoter by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): PromoterEntity?

    /** Batch-fetches promoters by ID — used to resolve embedded promoters for an event page. */
    fun findByIdIn(ids: Collection<Long>): Flow<PromoterEntity>
}
