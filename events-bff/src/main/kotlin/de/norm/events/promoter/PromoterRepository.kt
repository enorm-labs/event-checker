package de.norm.events.promoter

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PromoterRepository : CoroutineCrudRepository<PromoterEntity, Long> {
    /** Finds a single promoter by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): PromoterEntity?

    /** Batch-fetches promoters by ID — the event page's embedded promoters, and the list page's rows. */
    fun findByIdIn(ids: Collection<Long>): Flow<PromoterEntity>
}
