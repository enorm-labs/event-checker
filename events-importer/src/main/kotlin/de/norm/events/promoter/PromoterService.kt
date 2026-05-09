package de.norm.events.promoter

import de.norm.events.slug.SlugGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * Service encapsulating promoter business logic.
 *
 * Slugs are always auto-generated from the promoter name using [SlugGenerator].
 */
@Service
class PromoterService(
    private val promoterRepository: PromoterRepository
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Lists promoters with pagination and sorting.
     *
     * Pagination and sorting are controlled by the [pageable] parameter, which Spring
     * resolves from `page`, `size`, and `sort` query parameters
     * (e.g. `?page=0&size=20&sort=name,asc`).
     */
    fun findAll(pageable: Pageable): Flow<PromoterResponse> = promoterRepository.findAllBy(pageable).map { PromoterResponse.fromDomain(it.toDomain()) }

    /**
     * Finds a single promoter by [id].
     *
     * @throws PromoterNotFoundException if no promoter with the given [id] exists.
     */
    suspend fun findById(id: Long): PromoterResponse =
        PromoterResponse.fromDomain(promoterRepository.findById(id)?.toDomain() ?: throw PromoterNotFoundException(id))

    /**
     * Creates a new promoter.
     *
     * The slug is auto-generated from the promoter name
     * (e.g. `"36 Concerts"` → `"36-concerts"`).
     */
    suspend fun create(request: PromoterRequest): PromoterResponse {
        // Route through the domain model to ensure any future business rules
        // (e.g. validation, normalization) are consistently applied (see ADR-003).
        val promoter =
            Promoter(
                name = request.name,
                slug = SlugGenerator.slugify(request.name),
                websiteUrl = request.websiteUrl,
                imageUrl = request.imageUrl
            )
        val entity = PromoterEntity.fromDomain(promoter)
        val saved = promoterRepository.save(entity)
        logger.info { "Created promoter '${saved.name}' with id ${saved.id}" }
        return PromoterResponse.fromDomain(saved.toDomain())
    }

    /**
     * Replaces all mutable fields of an existing promoter.
     *
     * @throws PromoterNotFoundException if no promoter with the given [id] exists.
     */
    suspend fun update(
        id: Long,
        request: PromoterRequest
    ): PromoterResponse {
        val existing =
            promoterRepository.findById(id)
                ?: throw PromoterNotFoundException(id)

        val updated =
            existing.copy(
                name = request.name,
                slug = SlugGenerator.slugify(request.name),
                websiteUrl = request.websiteUrl,
                imageUrl = request.imageUrl
            )
        val saved = promoterRepository.save(updated)
        logger.info { "Updated promoter '${saved.name}' (id=${saved.id})" }
        return PromoterResponse.fromDomain(saved.toDomain())
    }

    /**
     * Deletes a promoter by [id].
     *
     * @throws PromoterNotFoundException if no promoter with the given [id] exists.
     */
    suspend fun delete(id: Long) {
        if (!promoterRepository.existsById(id)) throw PromoterNotFoundException(id)
        promoterRepository.deleteById(id)
        logger.info { "Deleted promoter with id $id" }
    }
}
