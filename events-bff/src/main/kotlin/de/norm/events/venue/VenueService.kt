package de.norm.events.venue

import de.norm.events.common.PageResponse
import de.norm.events.common.sanitizeSort
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
        val safePageable = pageable.sanitizeSort(SORTABLE_PROPERTIES, DEFAULT_SORT)
        val (entities, total) =
            if (query.isNullOrBlank()) {
                venueRepository.findAllBy(safePageable).toList() to venueRepository.count()
            } else {
                venueRepository.findByNameContainingIgnoreCase(query, safePageable).toList() to
                    venueRepository.countByNameContainingIgnoreCase(query)
            }
        return PageResponse.of(entities.map { VenueSummaryResponse.fromEntity(it) }, safePageable, total)
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

    companion object {
        /** Entity properties a client may sort the venue list by; anything else is ignored. */
        private val SORTABLE_PROPERTIES = setOf("name", "slug", "city")
        private val DEFAULT_SORT = Sort.by("name")
    }
}
