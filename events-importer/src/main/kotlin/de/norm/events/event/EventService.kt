package de.norm.events.event

import de.norm.events.artist.ArtistNotFoundException
import de.norm.events.artist.ArtistRepository
import de.norm.events.promoter.PromoterNotFoundException
import de.norm.events.promoter.PromoterRepository
import de.norm.events.slug.SlugGenerator
import de.norm.events.venue.VenueNotFoundException
import de.norm.events.venue.VenueRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service encapsulating event business logic.
 *
 * Events are the core entity linking venues, artists, and promoters.
 * Create and update operations are transactional because they span multiple
 * tables (`event`, `event_artist`, `event_promoter`). Slugs are auto-generated
 * from the event date and title using [SlugGenerator] when not provided.
 */
@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventArtistRepository: EventArtistRepository,
    private val eventPromoterRepository: EventPromoterRepository,
    private val venueRepository: VenueRepository,
    private val artistRepository: ArtistRepository,
    private val promoterRepository: PromoterRepository
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Lists events with their artist and promoter associations, applying pagination and sorting.
     *
     * Pagination and sorting are controlled by the [pageable] parameter, which Spring
     * resolves from `page`, `size`, and `sort` query parameters
     * (e.g. `?page=0&size=20&sort=eventDate,desc`).
     *
     * Uses batch loading to avoid N+1 queries: fetches the current page of events first,
     * then bulk-fetches all artist and promoter associations for that page in just
     * 2 additional queries. This results in exactly 3 queries per page regardless
     * of the number of events.
     */
    @Transactional(readOnly = true)
    suspend fun findAll(pageable: Pageable): List<EventResponse> {
        val entities = eventRepository.findAllBy(pageable).toList()
        if (entities.isEmpty()) return emptyList()

        val eventIds = entities.map { it.id!! }

        // Batch-fetch all associations in 2 queries, grouped by event ID
        val artistsByEventId = eventArtistRepository.findByEventIdIn(eventIds).toList().groupBy { it.eventId }
        val promotersByEventId = eventPromoterRepository.findByEventIdIn(eventIds).toList().groupBy { it.eventId }

        return entities.map { entity ->
            val artists = artistsByEventId[entity.id]?.map { EventArtistResponse.fromEntity(it) } ?: emptyList()
            val promoters = promotersByEventId[entity.id]?.map { it.promoterId } ?: emptyList()

            toResponse(entity, artists, promoters)
        }
    }

    /**
     * Finds a single event by [id], fully assembled with artist and promoter associations.
     *
     * @throws EventNotFoundException if no event with the given [id] exists.
     */
    @Transactional(readOnly = true)
    suspend fun findById(id: Long): EventResponse {
        val entity = eventRepository.findById(id) ?: throw EventNotFoundException(id)
        return toResponse(entity)
    }

    /**
     * Creates a new event with its artist and promoter associations.
     *
     * Validates that the referenced venue, artists, and promoters exist before
     * persisting. Slug is auto-generated from date and title if not provided
     * (e.g. `"2026-06-12-the-adicts"`).
     *
     * @throws VenueNotFoundException if the referenced venue does not exist.
     * @throws ArtistNotFoundException if any referenced artist does not exist.
     * @throws PromoterNotFoundException if any referenced promoter does not exist.
     */
    @Transactional
    suspend fun create(request: EventRequest): EventResponse {
        // Validate that referenced venue exists (existsById avoids fetching all columns)
        if (!venueRepository.existsById(request.venueId)) throw VenueNotFoundException(request.venueId)

        val slug = SlugGenerator.slugify("${request.eventDate}-${request.title}")
        val entity =
            EventEntity(
                venueId = request.venueId,
                title = request.title,
                subtitle = request.subtitle,
                description = request.description,
                eventType = request.eventType.name,
                status = request.status.name,
                slug = slug,
                eventDate = request.eventDate,
                doorsTime = request.doorsTime,
                startTime = request.startTime,
                imageUrl = request.imageUrl,
                sourceUrl = request.sourceUrl,
                sourceId = request.sourceId,
                ticketUrl = request.ticketUrl,
                facebookEventUrl = request.facebookEventUrl,
                genre = request.genre,
                pricePresale = request.pricePresale,
                priceBoxOffice = request.priceBoxOffice,
                priceCurrency = request.priceCurrency,
                priceNote = request.priceNote,
                soldOut = request.soldOut
            )
        val saved = eventRepository.save(entity)
        val eventId = saved.id!!

        val artistResponses = saveArtistAssociations(eventId, request.artists)
        val promoterIdResponses = savePromoterAssociations(eventId, request.promoterIds)

        logger.info { "Created event '${saved.title}' with id $eventId" }
        return toResponse(saved, artistResponses, promoterIdResponses)
    }

    /**
     * Replaces all fields and associations of an existing event.
     *
     * Artist and promoter associations are replaced using a delete-and-reinsert
     * strategy within the same transaction.
     *
     * @throws EventNotFoundException if no event with the given [id] exists.
     * @throws VenueNotFoundException if the referenced venue does not exist.
     * @throws ArtistNotFoundException if any referenced artist does not exist.
     * @throws PromoterNotFoundException if any referenced promoter does not exist.
     */
    @Transactional
    suspend fun update(
        id: Long,
        request: EventRequest
    ): EventResponse {
        val existing = eventRepository.findById(id) ?: throw EventNotFoundException(id)
        if (!venueRepository.existsById(request.venueId)) throw VenueNotFoundException(request.venueId)

        val slug = SlugGenerator.slugify("${request.eventDate}-${request.title}")
        val updated =
            existing.copy(
                venueId = request.venueId,
                title = request.title,
                subtitle = request.subtitle,
                description = request.description,
                eventType = request.eventType.name,
                status = request.status.name,
                slug = slug,
                eventDate = request.eventDate,
                doorsTime = request.doorsTime,
                startTime = request.startTime,
                imageUrl = request.imageUrl,
                sourceUrl = request.sourceUrl,
                sourceId = request.sourceId,
                ticketUrl = request.ticketUrl,
                facebookEventUrl = request.facebookEventUrl,
                genre = request.genre,
                pricePresale = request.pricePresale,
                priceBoxOffice = request.priceBoxOffice,
                priceCurrency = request.priceCurrency,
                priceNote = request.priceNote,
                soldOut = request.soldOut
            )
        val saved = eventRepository.save(updated)

        // Replace artist associations: delete existing, insert new
        eventArtistRepository.deleteByEventId(id)
        val artistResponses = saveArtistAssociations(id, request.artists)

        // Replace promoter associations: delete existing, insert new
        eventPromoterRepository.deleteByEventId(id)
        val promoterIdResponses = savePromoterAssociations(id, request.promoterIds)

        logger.info { "Updated event '${saved.title}' (id=$id)" }
        return toResponse(saved, artistResponses, promoterIdResponses)
    }

    /**
     * Deletes an event and its associations by [id].
     *
     * @throws EventNotFoundException if no event with the given [id] exists.
     */
    @Transactional
    suspend fun delete(id: Long) {
        if (!eventRepository.existsById(id)) throw EventNotFoundException(id)
        // Join table rows (event_artist, event_promoter) are cascade-deleted
        // by the database FK constraints (ON DELETE CASCADE).
        eventRepository.deleteById(id)
        logger.info { "Deleted event with id $id" }
    }

    /**
     * Validates that all referenced artists exist, persists the associations,
     * and returns the corresponding [EventArtistResponse] list for the caller.
     *
     * Uses batch validation via [findAllById] to check all artist IDs in a single query
     * instead of validating each one sequentially.
     */
    private suspend fun saveArtistAssociations(
        eventId: Long,
        artists: List<EventArtistRequest>
    ): List<EventArtistResponse> {
        if (artists.isEmpty()) return emptyList()

        // Batch-validate all referenced artists in a single query
        val requestedIds = artists.map { it.artistId }.toSet()
        val existingIds =
            artistRepository
                .findAllById(requestedIds)
                .toList()
                .map { it.id!! }
                .toSet()
        val missingIds = requestedIds - existingIds
        if (missingIds.isNotEmpty()) throw ArtistNotFoundException(missingIds.first())

        // Batch-persist all associations in a single saveAll call
        val entities =
            artists.map { artistReq ->
                EventArtistEntity(
                    eventId = eventId,
                    artistId = artistReq.artistId,
                    role = artistReq.role.name,
                    billingOrder = artistReq.billingOrder
                )
            }
        eventArtistRepository.saveAll(entities).toList()

        return artists.map { EventArtistResponse(artistId = it.artistId, role = it.role, billingOrder = it.billingOrder) }
    }

    /**
     * Validates that all referenced promoters exist, persists the associations,
     * and returns the promoter IDs for the caller.
     *
     * Uses batch validation via [findAllById] to check all promoter IDs in a single query
     * instead of validating each one sequentially.
     */
    private suspend fun savePromoterAssociations(
        eventId: Long,
        promoterIds: List<Long>
    ): List<Long> {
        if (promoterIds.isEmpty()) return emptyList()

        // Batch-validate all referenced promoters in a single query
        val requestedIds = promoterIds.toSet()
        val existingIds =
            promoterRepository
                .findAllById(requestedIds)
                .toList()
                .map { it.id!! }
                .toSet()
        val missingIds = requestedIds - existingIds
        if (missingIds.isNotEmpty()) throw PromoterNotFoundException(missingIds.first())

        // Batch-persist all associations in a single saveAll call
        val entities =
            promoterIds.map { promoterId ->
                EventPromoterEntity(eventId = eventId, promoterId = promoterId)
            }
        eventPromoterRepository.saveAll(entities).toList()

        return promoterIds
    }

    /**
     * Resolves an [EventEntity]'s artist and promoter associations and delegates
     * to [EventResponse.fromEntity] for the actual mapping.
     *
     * When [artistResponses] and [promoterIds] are provided (e.g. after create/update),
     * they are used directly to avoid re-querying within the same transaction.
     */
    private suspend fun toResponse(
        entity: EventEntity,
        artistResponses: List<EventArtistResponse>? = null,
        promoterIds: List<Long>? = null
    ): EventResponse {
        val artists =
            artistResponses ?: eventArtistRepository.findByEventId(entity.id!!).toList().map { EventArtistResponse.fromEntity(it) }
        val promoters = promoterIds ?: eventPromoterRepository.findByEventId(entity.id!!).toList().map { it.promoterId }

        return EventResponse.fromEntity(entity, artists, promoters)
    }
}
