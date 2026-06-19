package de.norm.events.venue

import de.norm.events.common.PageResponse
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read service for venues backing the venue list and detail pages.
 */
@Service
class VenueService(
    private val venueRepository: VenueRepository
) {
    /**
     * Lists venues with pagination, optionally filtered by a case-insensitive name [query].
     */
    @Transactional(readOnly = true)
    suspend fun list(
        query: String?,
        pageable: Pageable
    ): PageResponse<VenueSummaryResponse> {
        val (entities, total) =
            if (query.isNullOrBlank()) {
                venueRepository.findAllBy(pageable).toList() to venueRepository.count()
            } else {
                venueRepository.findByNameContainingIgnoreCase(query, pageable).toList() to
                    venueRepository.countByNameContainingIgnoreCase(query)
            }
        return PageResponse.of(entities.map { VenueSummaryResponse.fromEntity(it) }, pageable, total)
    }

    /**
     * Finds a single venue by [slug].
     *
     * @throws VenueNotFoundException if no venue with the given slug exists.
     */
    @Transactional(readOnly = true)
    suspend fun findBySlug(slug: String): VenueDetailResponse {
        val entity = venueRepository.findBySlug(slug) ?: throw VenueNotFoundException(slug)
        return VenueDetailResponse.fromEntity(entity)
    }
}
