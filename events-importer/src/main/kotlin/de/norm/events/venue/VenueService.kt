package de.norm.events.venue

import de.norm.events.slug.SlugGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * Service encapsulating venue business logic.
 *
 * All methods are suspending to align with the reactive R2DBC stack.
 * Slugs are always auto-generated from the venue name using [SlugGenerator].
 */
@Service
class VenueService(
    private val venueRepository: VenueRepository
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Lists venues with pagination and sorting.
     *
     * Pagination and sorting are controlled by the [pageable] parameter, which Spring
     * resolves from `page`, `size`, and `sort` query parameters
     * (e.g. `?page=0&size=20&sort=name,asc`).
     */
    fun findAll(pageable: Pageable): Flow<VenueResponse> = venueRepository.findAllBy(pageable).map { VenueResponse.fromDomain(it.toDomain()) }

    /**
     * Finds a single venue by [id].
     *
     * @throws VenueNotFoundException if no venue with the given [id] exists.
     */
    suspend fun findById(id: Long): VenueResponse = VenueResponse.fromDomain(venueRepository.findById(id)?.toDomain() ?: throw VenueNotFoundException(id))

    /**
     * Creates a new venue.
     *
     * The slug is auto-generated from the venue name
     * (e.g. `"Astra Kulturhaus"` → `"astra-kulturhaus"`).
     */
    suspend fun create(request: VenueRequest): VenueResponse {
        // Route through the domain model to ensure any future business rules
        // (e.g. validation, normalization) are consistently applied (see ADR-003).
        val venue =
            Venue(
                name = request.name,
                slug = SlugGenerator.slugify(request.name),
                address = request.address,
                city = request.city,
                postalCode = request.postalCode,
                latitude = request.latitude,
                longitude = request.longitude,
                websiteUrl = request.websiteUrl,
                imageUrl = request.imageUrl
            )
        val entity = VenueEntity.fromDomain(venue)
        val saved = venueRepository.save(entity)
        logger.info { "Created venue '${saved.name}' with id ${saved.id}" }
        return VenueResponse.fromDomain(saved.toDomain())
    }

    /**
     * Replaces all mutable fields of an existing venue.
     *
     * @throws VenueNotFoundException if no venue with the given [id] exists.
     */
    suspend fun update(
        id: Long,
        request: VenueRequest
    ): VenueResponse {
        val existing =
            venueRepository.findById(id)
                ?: throw VenueNotFoundException(id)

        val updated =
            existing.copy(
                name = request.name,
                slug = SlugGenerator.slugify(request.name),
                address = request.address,
                city = request.city,
                postalCode = request.postalCode,
                latitude = request.latitude,
                longitude = request.longitude,
                websiteUrl = request.websiteUrl,
                imageUrl = request.imageUrl
            )
        val saved = venueRepository.save(updated)
        logger.info { "Updated venue '${saved.name}' (id=${saved.id})" }
        return VenueResponse.fromDomain(saved.toDomain())
    }

    /**
     * Deletes a venue by [id].
     *
     * @throws VenueNotFoundException if no venue with the given [id] exists.
     */
    suspend fun delete(id: Long) {
        if (!venueRepository.existsById(id)) throw VenueNotFoundException(id)
        venueRepository.deleteById(id)
        logger.info { "Deleted venue with id $id" }
    }
}
