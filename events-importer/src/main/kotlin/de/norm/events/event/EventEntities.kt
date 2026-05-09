package de.norm.events.event

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * R2DBC entity mapped to the `event` table.
 *
 * Stores only the event's own columns (including `venue_id` as a FK).
 * Artist and promoter associations are managed through separate join-table entities.
 */
@Table("event", schema = "events")
data class EventEntity(
    @Id val id: Long? = null,
    val venueId: Long,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val eventType: String = EventType.CONCERT.name,
    val status: String = EventStatus.SCHEDULED.name,
    val slug: String,
    val eventDate: LocalDate,
    val doorsTime: LocalTime? = null,
    val startTime: LocalTime? = null,
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val sourceId: String,
    val ticketUrl: String? = null,
    val facebookEventUrl: String? = null,
    val genre: String? = null,
    val pricePresale: BigDecimal? = null,
    val priceBoxOffice: BigDecimal? = null,
    val priceCurrency: String = "EUR",
    val priceNote: String? = null,
    val soldOut: Boolean = false,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

/**
 * R2DBC entity mapped to the `event_artist` join table.
 */
@Table("event_artist", schema = "events")
data class EventArtistEntity(
    @Id val id: Long? = null,
    val eventId: Long,
    val artistId: Long,
    val role: String = ArtistRole.HEADLINER.name,
    val billingOrder: Int = 0
)

/**
 * R2DBC entity mapped to the `event_promoter` join table.
 *
 * Uses a surrogate auto-generated `id` column so that Spring Data R2DBC
 * correctly detects new entities (id = null → INSERT). A unique constraint
 * on (event_id, promoter_id) preserves the logical composite key.
 */
@Table("event_promoter", schema = "events")
data class EventPromoterEntity(
    @Id val id: Long? = null,
    val eventId: Long,
    val promoterId: Long
)
