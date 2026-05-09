package de.norm.events.promoter

import java.time.Instant

/**
 * Represents an event promoter or presenter (e.g. "36 Concerts").
 *
 * Promoters are often shared across multiple events and venues,
 * so they are stored as a separate entity.
 */
data class Promoter(
    /** Database primary key, `null` before persistence. Example: `3` */
    val id: Long? = null,
    /** Display name of the promoter. Example: `"36 Concerts"` */
    val name: String,
    /** URL-friendly identifier, derived from the name. Example: `"36-concerts"` */
    val slug: String,
    /** URL of the promoter's website or social page. Example: `"https://www.facebook.com/36Concerts/"` */
    val websiteUrl: String? = null,
    /** URL of the promoter's logo image. Example: `"https://example.com/36-concerts-logo.jpg"` */
    val imageUrl: String? = null,
    /** Timestamp when this record was first created. Set by the database. */
    val createdAt: Instant? = null,
    /** Timestamp when this record was last modified. Set by the database. */
    val updatedAt: Instant? = null
)
