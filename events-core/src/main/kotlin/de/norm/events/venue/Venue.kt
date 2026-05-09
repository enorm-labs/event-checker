package de.norm.events.venue

import java.math.BigDecimal
import java.time.Instant

/**
 * Represents a physical venue where music events take place.
 *
 * Each venue maps to a real-world location (e.g. "Astra Kulturhaus") and
 * serves as the anchor for importing events from that venue's website.
 */
data class Venue(
    /** Database primary key, `null` before persistence. Example: `42` */
    val id: Long? = null,
    /** Display name of the venue. Example: `"Astra Kulturhaus"` */
    val name: String,
    /** URL-friendly identifier, derived from the name. Example: `"astra-kulturhaus"` */
    val slug: String,
    /** Street address of the venue. Example: `"Revaler Str. 99"` */
    val address: String? = null,
    /** City where the venue is located. Example: `"Berlin"` */
    val city: String = "Berlin",
    /** Postal code of the venue's address. Example: `"10245"` */
    val postalCode: String? = null,
    /** Geographic latitude for map display. Example: `52.507242` */
    val latitude: BigDecimal? = null,
    /** Geographic longitude for map display. Example: `13.451803` */
    val longitude: BigDecimal? = null,
    /** URL of the venue's official website. Example: `"https://www.astra-berlin.de"` */
    val websiteUrl: String? = null,
    /** URL of the venue's logo or photo. Example: `"https://example.com/astra-logo.jpg"` */
    val imageUrl: String? = null,
    /** Timestamp when this record was first created. Set by the database. */
    val createdAt: Instant? = null,
    /** Timestamp when this record was last modified. Set by the database. */
    val updatedAt: Instant? = null
)
