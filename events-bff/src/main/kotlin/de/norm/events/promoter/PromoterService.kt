package de.norm.events.promoter

import de.norm.events.common.PageResponse
import de.norm.events.common.sanitizeSort
import de.norm.events.image.CachedImageGate
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
    private val promoterRepository: PromoterRepository,
    private val cachedImageGate: CachedImageGate
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
        val images = cachedImageGate.forUrls(entities.map { it.imageUrl })
        return PageResponse.of(
            entities.map { PromoterSummaryResponse.fromEntity(it, images.serve(it.imageUrl, CARD_WIDTH)) },
            safePageable,
            total
        )
    }

    /**
     * Finds a single promoter by [slug].
     *
     * @throws PromoterNotFoundException if no promoter with the given slug exists.
     */
    @Transactional(readOnly = true)
    suspend fun findBySlug(slug: String): PromoterDetailResponse {
        val entity = promoterRepository.findBySlug(slug) ?: throw PromoterNotFoundException(slug)
        val image = cachedImageGate.forUrls(listOf(entity.imageUrl)).serve(entity.imageUrl, DETAIL_WIDTH)
        return PromoterDetailResponse.fromEntity(entity, image)
    }

    companion object {
        /**
         * What the site draws one of these at, in CSS pixels.
         *
         * `BaseDetailView` leads with the picture at the full width of a `max-w-3xl` column, 704 px
         * after padding, and its `sizes` attribute states the same number. The list renders no image
         * today, so [CARD_WIDTH] is the thumbnail width `EventService` already uses for the same
         * kind of slot, ready for a card that arrives later.
         *
         * **CSS pixels, not file widths.** The device pixel ratio is the browser's to know, and it
         * picks from the `srcset` this produces.
         */
        private const val CARD_WIDTH = 96
        private const val DETAIL_WIDTH = 704

        /** Entity properties a client may sort the promoter list by; anything else is ignored. */
        private val SORTABLE_PROPERTIES = setOf("name", "slug")
        private val DEFAULT_SORT = Sort.by("name")
    }
}
