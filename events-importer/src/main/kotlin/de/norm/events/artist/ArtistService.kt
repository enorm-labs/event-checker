package de.norm.events.artist

import de.norm.events.slug.SlugGenerator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * Service encapsulating artist business logic.
 *
 * Slugs are always auto-generated from the artist name using [SlugGenerator].
 */
@Service
class ArtistService(
    private val artistRepository: ArtistRepository
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Lists artists with pagination and sorting.
     *
     * Pagination and sorting are controlled by the [pageable] parameter, which Spring
     * resolves from `page`, `size`, and `sort` query parameters
     * (e.g. `?page=0&size=20&sort=name,asc`).
     */
    fun findAll(pageable: Pageable): Flow<ArtistResponse> = artistRepository.findAllBy(pageable).map { ArtistResponse.fromDomain(it.toDomain()) }

    /**
     * Finds a single artist by [id].
     *
     * @throws ArtistNotFoundException if no artist with the given [id] exists.
     */
    suspend fun findById(id: Long): ArtistResponse = ArtistResponse.fromDomain(artistRepository.findById(id)?.toDomain() ?: throw ArtistNotFoundException(id))

    /**
     * Creates a new artist.
     *
     * The slug is auto-generated from the artist name
     * (e.g. `"The Adicts"` → `"the-adicts"`).
     */
    suspend fun create(request: ArtistRequest): ArtistResponse {
        val slug = SlugGenerator.slugify(request.name)
        // Pre-check for slug uniqueness to provide a clear error message instead of
        // relying on the DB constraint, which would produce a generic 409 response.
        ensureSlugAvailable(request.name, slug)

        // Route through the domain model to ensure any future business rules
        // (e.g. validation, normalization) are consistently applied (see ADR-003).
        val artist =
            Artist(
                name = request.name,
                slug = slug,
                description = request.description,
                imageUrl = request.imageUrl,
                websiteUrl = request.websiteUrl,
                facebookUrl = request.facebookUrl,
                instagramUrl = request.instagramUrl,
                youtubeUrl = request.youtubeUrl
            )
        val entity = ArtistEntity.fromDomain(artist)
        val saved = artistRepository.save(entity)
        logger.info { "Created artist '${saved.name}' with id ${saved.id}" }
        return ArtistResponse.fromDomain(saved.toDomain())
    }

    /**
     * Replaces all mutable fields of an existing artist.
     *
     * @throws ArtistNotFoundException if no artist with the given [id] exists.
     */
    suspend fun update(
        id: Long,
        request: ArtistRequest
    ): ArtistResponse {
        val existing =
            artistRepository.findById(id)
                ?: throw ArtistNotFoundException(id)

        val slug = SlugGenerator.slugify(request.name)
        // Only check for conflicts if the slug actually changed — renaming to the same
        // effective slug (e.g. fixing capitalization) should not trigger a false positive.
        if (slug != existing.slug) {
            ensureSlugAvailable(request.name, slug)
        }

        val updated =
            existing.copy(
                name = request.name,
                slug = slug,
                description = request.description,
                imageUrl = request.imageUrl,
                websiteUrl = request.websiteUrl,
                facebookUrl = request.facebookUrl,
                instagramUrl = request.instagramUrl,
                youtubeUrl = request.youtubeUrl
            )
        val saved = artistRepository.save(updated)
        logger.info { "Updated artist '${saved.name}' (id=${saved.id})" }
        return ArtistResponse.fromDomain(saved.toDomain())
    }

    /**
     * Deletes an artist by [id].
     *
     * @throws ArtistNotFoundException if no artist with the given [id] exists.
     */
    suspend fun delete(id: Long) {
        if (!artistRepository.existsById(id)) throw ArtistNotFoundException(id)
        // Join table rows (event_artist) are cascade-deleted by the database FK constraint (ON DELETE CASCADE).
        artistRepository.deleteById(id)
        logger.info { "Deleted artist with id $id" }
    }

    /**
     * Checks whether the given [slug] is already taken by another artist and throws
     * [DuplicateArtistSlugException] if so. This provides a clear, domain-specific error
     * message to the API consumer instead of relying on the DB's generic constraint violation.
     */
    private suspend fun ensureSlugAvailable(
        name: String,
        slug: String
    ) {
        if (artistRepository.findBySlug(slug) != null) {
            throw DuplicateArtistSlugException(name, slug)
        }
    }
}
