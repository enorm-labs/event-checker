package de.norm.events.promoter

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive repository for [Promoter][de.norm.events.promoter.Promoter] persistence via R2DBC.
 */
interface PromoterRepository : CoroutineCrudRepository<PromoterEntity, Long> {
    suspend fun findBySlug(slug: String): PromoterEntity?

    /** Batch-fetches promoters by their slugs. Used by the import pipeline to avoid N+1 queries. */
    fun findBySlugIn(slugs: Collection<String>): Flow<PromoterEntity>

    /** Finds all promoters with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<PromoterEntity>
}
