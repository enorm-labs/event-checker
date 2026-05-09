package de.norm.events.venue

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive repository for [Venue][de.norm.events.venue.Venue] persistence via R2DBC.
 *
 * Uses Spring Data's [CoroutineCrudRepository] so all operations are suspending
 * functions, fitting naturally into the coroutine-based WebFlux stack.
 */
interface VenueRepository : CoroutineCrudRepository<VenueEntity, Long> {
    suspend fun findBySlug(slug: String): VenueEntity?

    /** Finds all venues with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<VenueEntity>
}
