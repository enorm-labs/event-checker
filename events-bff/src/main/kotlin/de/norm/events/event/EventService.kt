package de.norm.events.event

import de.norm.events.artist.ArtistEntity
import de.norm.events.artist.ArtistRepository
import de.norm.events.artist.ArtistSummaryResponse
import de.norm.events.common.PageResponse
import de.norm.events.event.EventService.Companion.MAX_CALENDAR_DAYS
import de.norm.events.genretag.EventGenreTagRepository
import de.norm.events.genretag.GenreTagRepository
import de.norm.events.promoter.PromoterRepository
import de.norm.events.promoter.PromoterSummaryResponse
import de.norm.events.venue.VenueRepository
import de.norm.events.venue.VenueSummaryResponse
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

/**
 * Read service assembling the public event responses for the frontend.
 *
 * List/calendar/today responses use batch loading to avoid N+1 queries: a page of events is
 * resolved first, then venue, artist, promoter, and genre tag associations are bulk-fetched
 * for the whole page (mirroring the importer's [de.norm.events.event] strategy).
 */
@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventSearchRepository: EventSearchRepository,
    private val eventArtistRepository: EventArtistRepository,
    private val eventPromoterRepository: EventPromoterRepository,
    private val eventGenreTagRepository: EventGenreTagRepository,
    private val venueRepository: VenueRepository,
    private val artistRepository: ArtistRepository,
    private val promoterRepository: PromoterRepository,
    private val genreTagRepository: GenreTagRepository
) {
    /**
     * Searches events with optional filters and pagination, returning summaries with
     * pagination metadata.
     */
    @Transactional(readOnly = true)
    suspend fun search(
        filter: EventFilter,
        pageable: Pageable
    ): PageResponse<EventSummaryResponse> {
        val page = eventSearchRepository.search(filter, pageable)
        val events = hydrateOrdered(page.ids)
        return PageResponse.of(summariesFor(events), pageable, page.total)
    }

    /** Today's events, ordered by start time — backs the Home page. */
    @Transactional(readOnly = true)
    suspend fun today(): List<EventSummaryResponse> = summariesFor(eventRepository.findByEventDateOrderByStartTime(LocalDate.now()).toList())

    /**
     * Events within an inclusive date range, for the calendar view.
     *
     * @throws ResponseStatusException 400 if the range is inverted or exceeds [MAX_CALENDAR_DAYS].
     */
    @Transactional(readOnly = true)
    suspend fun calendar(
        from: LocalDate,
        to: LocalDate
    ): List<EventSummaryResponse> {
        if (to.isBefore(from)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' must not be before 'from'")
        }
        if (from.plusDays(MAX_CALENDAR_DAYS) <= to) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range must not exceed $MAX_CALENDAR_DAYS days")
        }
        return summariesFor(eventRepository.findByEventDateBetweenOrderByEventDateAscStartTimeAsc(from, to).toList())
    }

    /**
     * Finds a single event by [slug], fully assembled with venue, lineup, promoters, and genre tags.
     *
     * @throws EventNotFoundException if no event with the given slug exists.
     */
    @Transactional(readOnly = true)
    suspend fun findBySlug(slug: String): EventDetailResponse {
        val event = eventRepository.findBySlug(slug) ?: throw EventNotFoundException(slug)
        val eventId = requireNotNull(event.id) { "Persisted event must have an ID" }

        val venue = venueRepository.findById(event.venueId)
        val venueSummary =
            requireNotNull(venue) { "Event $eventId references missing venue ${event.venueId}" }
                .let { VenueSummaryResponse.fromEntity(it) }

        val artistLinks = eventArtistRepository.findByEventId(eventId).toList().sortedBy { it.billingOrder }
        val artistsById = fetchArtists(artistLinks.map { it.artistId })
        val lineup =
            artistLinks.mapNotNull { link ->
                artistsById[link.artistId]?.let { artist ->
                    LineupEntryResponse(
                        artist = ArtistSummaryResponse.fromEntity(artist),
                        role = ArtistRole.parseOrDefault(link.role),
                        billingOrder = link.billingOrder,
                        stage = link.stage
                    )
                }
            }

        val promoterIds = eventPromoterRepository.findByEventId(eventId).toList().map { it.promoterId }
        val promoters =
            if (promoterIds.isEmpty()) {
                emptyList()
            } else {
                promoterRepository.findByIdIn(promoterIds).toList().map { PromoterSummaryResponse.fromEntity(it) }
            }

        val genreTagIds = eventGenreTagRepository.findByEventId(eventId).toList().map { it.genreTagId }
        val genreTags =
            if (genreTagIds.isEmpty()) emptyList() else genreTagRepository.findAllById(genreTagIds).toList().map { it.name }

        return EventDetailResponse.fromEntity(event, venueSummary, lineup, promoters, genreTags)
    }

    /** Re-fetches events by ID via the CRUD repository, preserving the order of [ids]. */
    private suspend fun hydrateOrdered(ids: List<Long>): List<EventEntity> {
        if (ids.isEmpty()) return emptyList()
        val byId = eventRepository.findAllById(ids).toList().associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    /**
     * Maps a page of events to summaries, batch-loading venue, artist, and genre tag
     * associations in a fixed number of queries regardless of page size.
     */
    private suspend fun summariesFor(events: List<EventEntity>): List<EventSummaryResponse> {
        if (events.isEmpty()) return emptyList()
        val eventIds = events.map { requireNotNull(it.id) { "Persisted event must have an ID" } }

        val venuesById = venueRepository.findByIdIn(events.map { it.venueId }.distinct()).toList().associateBy { it.id }

        val artistLinks = eventArtistRepository.findByEventIdIn(eventIds).toList()
        val artistsById = fetchArtists(artistLinks.map { it.artistId })
        val artistLinksByEvent = artistLinks.groupBy { it.eventId }

        val genreLinks = eventGenreTagRepository.findByEventIdIn(eventIds).toList()
        val genreNamesById =
            genreLinks.map { it.genreTagId }.distinct().let { tagIds ->
                if (tagIds.isEmpty()) emptyMap() else genreTagRepository.findAllById(tagIds).toList().associate { it.id!! to it.name }
            }
        val genreLinksByEvent = genreLinks.groupBy { it.eventId }

        return events.map { event ->
            val venue =
                requireNotNull(venuesById[event.venueId]) { "Event ${event.id} references missing venue ${event.venueId}" }
            val artistNames =
                artistLinksByEvent[event.id].orEmpty().sortedBy { it.billingOrder }.mapNotNull { artistsById[it.artistId]?.name }
            val genreTags = genreLinksByEvent[event.id].orEmpty().mapNotNull { genreNamesById[it.genreTagId] }
            EventSummaryResponse.fromEntity(event, VenueSummaryResponse.fromEntity(venue), artistNames, genreTags)
        }
    }

    private suspend fun fetchArtists(artistIds: List<Long>): Map<Long?, ArtistEntity> {
        val distinct = artistIds.distinct()
        return if (distinct.isEmpty()) emptyMap() else artistRepository.findByIdIn(distinct).toList().associateBy { it.id }
    }

    companion object {
        /** Maximum span (inclusive) the calendar endpoint will return in a single request. */
        private const val MAX_CALENDAR_DAYS = 92L
    }
}
