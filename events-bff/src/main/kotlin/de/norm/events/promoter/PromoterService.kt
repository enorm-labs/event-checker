package de.norm.events.promoter

import de.norm.events.common.PageResponse
import de.norm.events.common.sanitizeSort
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read service for promoters backing the promoter list and detail pages.
 */
@Service
class PromoterService(
    private val promoterRepository: PromoterRepository
) {
    /**
     * Lists promoters with pagination, optionally filtered by a case-insensitive name [query].
     */
    @Transactional(readOnly = true)
    suspend fun list(
        query: String?,
        pageable: Pageable
    ): PageResponse<PromoterSummaryResponse> {
        val safePageable = pageable.sanitizeSort(SORTABLE_PROPERTIES, DEFAULT_SORT)
        val (entities, total) =
            if (query.isNullOrBlank()) {
                promoterRepository.findAllBy(safePageable).toList() to promoterRepository.count()
            } else {
                promoterRepository.findByNameContainingIgnoreCase(query, safePageable).toList() to
                    promoterRepository.countByNameContainingIgnoreCase(query)
            }
        return PageResponse.of(entities.map { PromoterSummaryResponse.fromEntity(it) }, safePageable, total)
    }

    /**
     * Finds a single promoter by [slug].
     *
     * @throws PromoterNotFoundException if no promoter with the given slug exists.
     */
    @Transactional(readOnly = true)
    suspend fun findBySlug(slug: String): PromoterDetailResponse {
        val entity = promoterRepository.findBySlug(slug) ?: throw PromoterNotFoundException(slug)
        return PromoterDetailResponse.fromEntity(entity)
    }

    companion object {
        /** Entity properties a client may sort the promoter list by; anything else is ignored. */
        private val SORTABLE_PROPERTIES = setOf("name", "slug")
        private val DEFAULT_SORT = Sort.by("name")
    }
}
