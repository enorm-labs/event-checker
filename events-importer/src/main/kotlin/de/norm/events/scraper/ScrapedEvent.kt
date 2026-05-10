package de.norm.events.scraper

import de.norm.events.event.ArtistRole
import de.norm.events.event.EventArtistEntity
import de.norm.events.event.EventEntity
import de.norm.events.event.EventStatus
import de.norm.events.event.EventType
import de.norm.events.event.normalizeMoneyScale
import de.norm.events.slug.SlugGenerator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * Intermediate representation of a scraped event before domain mapping.
 *
 * Contains raw data extracted from a venue website, closely matching the
 * fields of [de.norm.events.event.EventEntity] but using simple types.
 * Artist and promoter names are captured as raw strings — the service
 * layer resolves them to database entities (auto-creating if necessary).
 */
data class ScrapedEvent(
    /** Main headline or name of the event. */
    val title: String,
    /** Secondary line, often a tour name or support acts. */
    val subtitle: String? = null,
    /** Longer description or artist biography. */
    val description: String? = null,
    /** Kind of event as categorized by the source (e.g. "CONCERT", "PARTY"). */
    val eventType: String = "CONCERT",
    /** Calendar date of the event. */
    val eventDate: LocalDate,
    /** Time when doors open to the public. */
    val doorsTime: LocalTime? = null,
    /** Time when the show/performance starts. */
    val startTime: LocalTime? = null,
    /** URL of the event's poster or flyer image. */
    val imageUrl: String? = null,
    /** Original URL of the event on the source website. */
    val sourceUrl: String,
    /**
     * Unique identifier for this event from the import source.
     * Used for idempotent upserts — format: `"<source-slug>:<event-identifier>"`.
     * Example: `"privatclub:2026-06-12-the-adicts"`.
     */
    val sourceId: String,
    /** URL to the external ticket shop. */
    val ticketUrl: String? = null,
    /** Music genre or style tag. */
    val genre: String? = null,
    /** Presale ticket price (Vorverkauf). */
    val pricePresale: BigDecimal? = null,
    /** Box office ticket price (Abendkasse). */
    val priceBoxOffice: BigDecimal? = null,
    /** Free-form pricing note for non-standard pricing (e.g. "donation 2-5€"). */
    val priceNote: String? = null,
    /** Whether all tickets are sold out. */
    val soldOut: Boolean = false,
    /** Scheduling status (e.g. "SCHEDULED", "CANCELLED", "POSTPONED", "RELOCATED"). */
    val status: String = "SCHEDULED",
    /**
     * Raw artist names extracted from the event listing.
     * Each pair contains the artist name and their role (e.g. "HEADLINER", "SUPPORT", "DJ").
     * The service layer resolves these to database artist entities.
     */
    val artists: List<ScrapedArtist> = emptyList()
) {
    /**
     * Converts this scraped event into an [EventEntity] for persistence.
     *
     * This is a pure mapping function with no I/O — the caller is responsible for persisting
     * the returned entity. The slug is always regenerated from the event date and title.
     * On updates, the [existing] entity's `id`, `sourceId`, and `createdAt` are preserved.
     *
     * @param venueId the database ID of the venue this event belongs to.
     * @param eventSourceId the database ID of the event source that imported this event.
     * @param existing the previously persisted entity for updates, or null for new events.
     */
    fun toEventEntity(
        venueId: Long,
        eventSourceId: Long,
        existing: EventEntity? = null
    ): EventEntity =
        // priceCurrency is intentionally omitted — all scraped venues are currently in Berlin
        // (EUR). EventEntity defaults to "EUR". If non-EUR venues are added, introduce a
        // priceCurrency field on ScrapedEvent and pass it through here.
        EventEntity(
            // Preserve id and sourceId from existing entity on updates; sourceId is the
            // immutable identity key for matching scraped events to persisted rows.
            id = existing?.id,
            sourceId = existing?.sourceId ?: sourceId,
            createdAt = existing?.createdAt,
            venueId = venueId,
            eventSourceId = eventSourceId,
            title = title,
            subtitle = subtitle,
            description = description,
            eventType = EventType.parseOrDefault(eventType).name,
            status = EventStatus.parseOrDefault(status).name,
            slug = SlugGenerator.slugify("$eventDate-$title"),
            eventDate = eventDate,
            doorsTime = doorsTime,
            startTime = startTime,
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            ticketUrl = ticketUrl,
            genre = genre,
            pricePresale = pricePresale?.normalizeMoneyScale(),
            priceBoxOffice = priceBoxOffice?.normalizeMoneyScale(),
            priceNote = priceNote,
            soldOut = soldOut
        )
}

/**
 * A raw artist reference extracted from a scraped event.
 */
data class ScrapedArtist(
    /** Artist or band name as it appears on the website. */
    val name: String,
    /** Role in the lineup (e.g. "HEADLINER", "SUPPORT", "DJ"). Defaults to headliner. */
    val role: String = "HEADLINER"
) {
    /**
     * Converts this scraped artist into an [EventArtistEntity] join-table entry.
     *
     * Parses the raw [role] string into a known [ArtistRole], falling back to
     * [ArtistRole.HEADLINER] for unrecognized values.
     *
     * @param eventId the database ID of the event this artist is linked to.
     * @param artistId the resolved database ID of the artist.
     * @param billingOrder the position in the lineup (0-based).
     */
    fun toEventArtistEntity(
        eventId: Long,
        artistId: Long,
        billingOrder: Int
    ): EventArtistEntity =
        EventArtistEntity(
            eventId = eventId,
            artistId = artistId,
            role = ArtistRole.parseOrDefault(role).name,
            billingOrder = billingOrder
        )
}
