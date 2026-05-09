package de.norm.events.artist

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive repository for [Artist][de.norm.events.artist.Artist] persistence via R2DBC.
 */
interface ArtistRepository : CoroutineCrudRepository<ArtistEntity, Long> {
    suspend fun findBySlug(slug: String): ArtistEntity?

    /** Finds all artists with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<ArtistEntity>
}
