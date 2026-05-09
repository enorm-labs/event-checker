package de.norm.events.artist

/**
 * Test fixture factory for [ArtistRequest] DTOs.
 *
 * ```kotlin
 * val request = ArtistRequestFixtures.adicts()
 * val request = ArtistRequestFixtures.create(name = "Maid of Ace")
 * ```
 */
object ArtistRequestFixtures {
    /** Default artist request modelled after The Adicts. */
    fun adicts(
        name: String = "The Adicts",
        description: String? = "Formed in Ipswich in the late 1970s",
        imageUrl: String? = null,
        websiteUrl: String? = "https://theadicts.net/",
        facebookUrl: String? = null,
        instagramUrl: String? = null,
        youtubeUrl: String? = null
    ): ArtistRequest =
        ArtistRequest(
            name = name,
            description = description,
            imageUrl = imageUrl,
            websiteUrl = websiteUrl,
            facebookUrl = facebookUrl,
            instagramUrl = instagramUrl,
            youtubeUrl = youtubeUrl
        )

    /** Creates an [ArtistRequest] with minimal defaults. */
    fun create(
        name: String = "Test Artist",
        description: String? = null,
        imageUrl: String? = null,
        websiteUrl: String? = null,
        facebookUrl: String? = null,
        instagramUrl: String? = null,
        youtubeUrl: String? = null
    ): ArtistRequest =
        ArtistRequest(
            name = name,
            description = description,
            imageUrl = imageUrl,
            websiteUrl = websiteUrl,
            facebookUrl = facebookUrl,
            instagramUrl = instagramUrl,
            youtubeUrl = youtubeUrl
        )
}
