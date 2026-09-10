package de.norm.events.artist

import java.time.Instant

/**
 * Represents a musical artist or band that performs at events.
 *
 * Artists are normalized separately so they can be linked to multiple events
 * and enriched with metadata (bio, social links, images) over time.
 */
data class Artist(
    /** Database primary key, `null` before persistence. */
    val id: Long? = null,
    /** Stage name or band name. */
    val name: String,
    /** URL-friendly identifier, derived from the name. Example: `"the-adicts"` */
    val slug: String,
    /** Biography or description text, often imported from venue pages. */
    val description: String? = null,
    /** URL of the artist's photo or band logo. */
    val imageUrl: String? = null,
    /** Who to credit for [imageUrl], worded as the archive publishes it. Null exactly when [imageUrl] is. */
    val imageAttribution: String? = null,
    /** SPDX identifier of the licence [imageUrl] is published under. Example: `"CC-BY-SA-4.0"` */
    val imageLicenceId: String? = null,
    /** The image's description page, which the rendered credit links to. */
    val imageSourceUrl: String? = null,
    /** URL of the artist's official homepage. */
    val websiteUrl: String? = null,
    /** URL of the artist's Facebook page. */
    val facebookUrl: String? = null,
    /** URL of the artist's Instagram profile. */
    val instagramUrl: String? = null,
    /** URL of the artist's YouTube channel. */
    val youtubeUrl: String? = null,
    /** Timestamp when this record was first created. Set by the database. */
    val createdAt: Instant? = null,
    /** Timestamp when this record was last modified. Set by the database. */
    val updatedAt: Instant? = null
)
