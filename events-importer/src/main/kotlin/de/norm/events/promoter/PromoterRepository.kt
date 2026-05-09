package de.norm.events.promoter

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive repository for [Promoter][de.norm.events.promoter.Promoter] persistence via R2DBC.
 */
interface PromoterRepository : CoroutineCrudRepository<PromoterEntity, Long> {
    suspend fun findBySlug(slug: String): PromoterEntity?

    /** Finds all promoters with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<PromoterEntity>
}
