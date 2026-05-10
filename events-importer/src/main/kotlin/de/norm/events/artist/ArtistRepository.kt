package de.norm.events.artist

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Reactive repository for [Artist][de.norm.events.artist.Artist] persistence via R2DBC.
 */
interface ArtistRepository : CoroutineCrudRepository<ArtistEntity, Long> {
    /** Finds all artists with pagination and sorting applied via [pageable]. */
    fun findAllBy(pageable: Pageable): Flow<ArtistEntity>

    /** Finds a single artist by its unique slug, or null if not found. */
    suspend fun findBySlug(slug: String): ArtistEntity?

    /** Batch-fetches artists by their slugs. Used by the import pipeline to avoid N+1 queries. */
    fun findBySlugIn(slugs: Collection<String>): Flow<ArtistEntity>
}
