package de.norm.events.common

import de.norm.events.artist.ArtistRequest
import de.norm.events.promoter.PromoterRequest
import de.norm.events.venue.VenueRequest
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation
import org.junit.jupiter.api.Test

/**
 * An image URL may not be stored without the credit that makes it displayable (#1275).
 *
 * The three columns are entered by hand, so nothing else checks them. A row accepted without a
 * credit is one the site then renders uncredited, which is the breach the columns exist to stop.
 * The database repeats the rule as a CHECK constraint; this is the half that produces a 400 with
 * the missing field named, rather than a 500.
 */
class AttributedImageTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    private fun rejected(request: Any): List<String> = validator.validate(request).map { it.propertyPath.toString() }.sorted()

    private fun venue(
        imageUrl: String? = "https://upload.wikimedia.org/example.jpg",
        imageAttribution: String? = "Photographer Name, via Wikimedia Commons",
        imageLicenceId: String? = "CC-BY-SA-4.0",
        imageSourceUrl: String? = "https://commons.wikimedia.org/wiki/File:Example.jpg"
    ) = VenueRequest(
        name = "Astra Kulturhaus",
        imageUrl = imageUrl,
        imageAttribution = imageAttribution,
        imageLicenceId = imageLicenceId,
        imageSourceUrl = imageSourceUrl
    )

    @Test
    fun `a fully credited image is accepted`() {
        rejected(venue()) shouldBe emptyList()
    }

    @Test
    fun `a venue with no image at all is accepted`() {
        rejected(venue(imageUrl = null, imageAttribution = null, imageLicenceId = null, imageSourceUrl = null)) shouldBe emptyList()
    }

    @Test
    fun `an image with no credit names every field it is missing`() {
        rejected(venue(imageAttribution = null, imageLicenceId = null, imageSourceUrl = null)) shouldBe
            listOf("imageAttribution", "imageLicenceId", "imageSourceUrl")
    }

    @Test
    fun `a blank credit is refused, because whitespace is not an author`() {
        rejected(venue(imageAttribution = "   ")) shouldBe listOf("imageAttribution")
    }

    @Test
    fun `a public-domain image still declares its licence`() {
        rejected(venue(imageLicenceId = "PD")) shouldBe emptyList()
        rejected(venue(imageLicenceId = null)) shouldBe listOf("imageLicenceId")
    }

    @Test
    fun `the rule reaches artists`() {
        rejected(ArtistRequest(name = "The Adicts", imageUrl = "https://example.com/adicts.jpg")) shouldBe
            listOf("imageAttribution", "imageLicenceId", "imageSourceUrl")
    }

    @Test
    fun `the rule reaches promoters`() {
        rejected(PromoterRequest(name = "36 Concerts", imageUrl = "https://example.com/logo.jpg")) shouldBe
            listOf("imageAttribution", "imageLicenceId", "imageSourceUrl")
    }
}
