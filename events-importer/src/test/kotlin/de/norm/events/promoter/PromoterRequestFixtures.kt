package de.norm.events.promoter

/**
 * Test fixture factory for [PromoterRequest] DTOs.
 *
 * ```kotlin
 * val request = PromoterRequestFixtures.concerts36()
 * val request = PromoterRequestFixtures.create(name = "Goodlive")
 * ```
 */
object PromoterRequestFixtures {
    /** Default promoter request modelled after 36 Concerts. */
    fun concerts36(
        name: String = "36 Concerts",
        websiteUrl: String? = "https://www.facebook.com/36Concerts/",
        imageUrl: String? = null,
        description: String? = "The in-house agency of Lido, Astra and Bi Nuu.",
        descriptionLanguage: String? = "en",
        descriptionAlt: String? = "Die Hausagentur von Lido, Astra und Bi Nuu.",
        descriptionAltLanguage: String? = "de"
    ): PromoterRequest =
        PromoterRequest(
            name = name,
            websiteUrl = websiteUrl,
            imageUrl = imageUrl,
            description = description,
            descriptionLanguage = descriptionLanguage,
            descriptionAlt = descriptionAlt,
            descriptionAltLanguage = descriptionAltLanguage
        )

    /** Creates a [PromoterRequest] with minimal defaults. */
    fun create(
        name: String = "Test Promoter",
        websiteUrl: String? = null,
        imageUrl: String? = null
    ): PromoterRequest =
        PromoterRequest(
            name = name,
            websiteUrl = websiteUrl,
            imageUrl = imageUrl
        )
}
