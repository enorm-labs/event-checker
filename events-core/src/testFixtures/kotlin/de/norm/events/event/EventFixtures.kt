package de.norm.events.event

import de.norm.events.artist.Artist
import de.norm.events.artist.ArtistFixtures
import de.norm.events.promoter.Promoter
import de.norm.events.promoter.PromoterFixtures
import de.norm.events.venue.Venue
import de.norm.events.venue.VenueFixtures
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Factory for creating [Event] and [LineupEntry] instances in tests.
 *
 * ```kotlin
 * val event = EventFixtures.create(title = "KAOS live")
 * val entry = EventFixtures.createLineupEntry(artist = ArtistFixtures.create(name = "Maid of Ace"))
 * ```
 */
object EventFixtures {
    fun create(
        id: Long? = null,
        venue: Venue = VenueFixtures.create(),
        lineup: List<LineupEntry> = listOf(createLineupEntry()),
        promoters: List<Promoter> = listOf(PromoterFixtures.create()),
        title: String = "THE ADICTS",
        subtitle: String? = "Adios Amigos Tour 2026",
        description: String? = null,
        eventType: EventType = EventType.CONCERT,
        slug: String = "2026-06-12-the-adicts",
        eventDate: LocalDate = LocalDate.of(2026, 6, 12),
        doorsTime: LocalTime? = LocalTime.of(19, 0),
        startTime: LocalTime? = LocalTime.of(20, 0),
        imageUrl: String? = null,
        sourceUrl: String? = "https://www.astra-berlin.de/events/2026-06-12-the-adicts",
        sourceId: String = "astra:2026-06-12-the-adicts",
        ticketUrl: String? = null,
        facebookEventUrl: String? = null,
        genre: String? = "Punk",
        status: EventStatus = EventStatus.SCHEDULED,
        pricePresale: BigDecimal? = BigDecimal("38.00"),
        priceBoxOffice: BigDecimal? = BigDecimal("45.00"),
        priceCurrency: String = "EUR",
        priceNote: String? = null,
        soldOut: Boolean = false,
        createdAt: Instant? = null,
        updatedAt: Instant? = null
    ): Event =
        Event(
            id = id,
            venue = venue,
            lineup = lineup,
            promoters = promoters,
            title = title,
            subtitle = subtitle,
            description = description,
            eventType = eventType,
            slug = slug,
            eventDate = eventDate,
            doorsTime = doorsTime,
            startTime = startTime,
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            sourceId = sourceId,
            ticketUrl = ticketUrl,
            facebookEventUrl = facebookEventUrl,
            genre = genre,
            status = status,
            pricePresale = pricePresale,
            priceBoxOffice = priceBoxOffice,
            priceCurrency = priceCurrency,
            priceNote = priceNote,
            soldOut = soldOut,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    fun createLineupEntry(
        artist: Artist = ArtistFixtures.create(),
        role: ArtistRole = ArtistRole.HEADLINER,
        billingOrder: Int = 0
    ): LineupEntry =
        LineupEntry(
            artist = artist,
            role = role,
            billingOrder = billingOrder
        )
}
