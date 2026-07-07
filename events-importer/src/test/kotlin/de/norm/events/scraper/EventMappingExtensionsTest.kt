package de.norm.events.scraper

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EventMappingExtensionsTest {
    // --- mapEventType ---

    @Test
    fun `mapEventType maps base German and English labels`() {
        mapEventType("Konzert") shouldBe "CONCERT"
        mapEventType("Concert") shouldBe "CONCERT"
        mapEventType("Festival") shouldBe "FESTIVAL"
        mapEventType("Party") shouldBe "PARTY"
        mapEventType("Sonstiges") shouldBe "OTHER"
    }

    @Test
    fun `mapEventType is case-insensitive and trims whitespace`() {
        mapEventType("KONZERT") shouldBe "CONCERT"
        mapEventType("  Konzert  ") shouldBe "CONCERT"
    }

    @Test
    fun `mapEventType returns null for null, blank and unknown labels`() {
        mapEventType(null).shouldBeNull()
        mapEventType("").shouldBeNull()
        mapEventType("   ").shouldBeNull()
        mapEventType("Workshop").shouldBeNull()
    }

    @Test
    fun `mapEventType prefers venue-specific synonyms over the base table`() {
        mapEventType("Live", mapOf("live" to "CONCERT")) shouldBe "CONCERT"
        // extra synonyms take precedence over the base mapping
        mapEventType("Party", mapOf("party" to "CLUB_NIGHT")) shouldBe "CLUB_NIGHT"
    }

    // --- extractSupportFromSubtitle ---

    @Test
    fun `extractSupportFromSubtitle returns empty when no support line`() {
        extractSupportFromSubtitle("Tour 2026").shouldBeEmpty()
        extractSupportFromSubtitle(null).shouldBeEmpty()
        extractSupportFromSubtitle("").shouldBeEmpty()
    }

    @Test
    fun `extractSupportFromSubtitle extracts a single support act`() {
        extractSupportFromSubtitle("Tour 2026 | Support: Luana") shouldContainExactly listOf("Luana")
    }

    @Test
    fun `extractSupportFromSubtitle splits multiple acts on common delimiters`() {
        extractSupportFromSubtitle("Tour + Support: High On Fire & Gnome, Aska") shouldContainExactly
            listOf("High On Fire", "Gnome", "Aska")
    }

    @Test
    fun `extractSupportFromSubtitle keeps a backing-band tail attached to its act`() {
        extractSupportFromSubtitle("Support: Scott Hepple & The Sun Band") shouldContainExactly
            listOf("Scott Hepple & The Sun Band")
    }

    // --- splitSupportActs ---

    @Test
    fun `splitSupportActs cuts on hard separators and guarded conjunctions`() {
        splitSupportActs("GUM + CLAVV") shouldContainExactly listOf("GUM", "CLAVV")
        splitSupportActs("High On Fire & Gnome, Aska") shouldContainExactly
            listOf("High On Fire", "Gnome", "Aska")
    }

    @Test
    fun `splitSupportActs splits the und conjunction but keeps the and-the-Ys tail attached`() {
        splitSupportActs("Earth Tongue und Scott Hepple & The Sun Band") shouldContainExactly
            listOf("Earth Tongue", "Scott Hepple & The Sun Band")
    }

    // --- isPlaceholderName ---

    @Test
    fun `isPlaceholderName returns true for TBA variants`() {
        isPlaceholderName("TBA") shouldBe true
        isPlaceholderName("tba") shouldBe true
        isPlaceholderName("TBA.") shouldBe true
        isPlaceholderName("T.B.A.") shouldBe true
    }

    @Test
    fun `isPlaceholderName returns true for TBD variants`() {
        isPlaceholderName("TBD") shouldBe true
        isPlaceholderName("tbd") shouldBe true
        isPlaceholderName("TBD.") shouldBe true
    }

    @Test
    fun `isPlaceholderName returns true for TBC variants`() {
        isPlaceholderName("TBC") shouldBe true
        isPlaceholderName("tbc") shouldBe true
        isPlaceholderName("TBC.") shouldBe true
    }

    @Test
    fun `isPlaceholderName returns true for NN variants`() {
        isPlaceholderName("N.N.") shouldBe true
        isPlaceholderName("NN") shouldBe true
        isPlaceholderName("nn") shouldBe true
        isPlaceholderName("NN.") shouldBe true
    }

    @Test
    fun `isPlaceholderName trims whitespace`() {
        isPlaceholderName("  TBA  ") shouldBe true
    }

    @Test
    fun `isPlaceholderName returns false for real artist names`() {
        isPlaceholderName("Aska") shouldBe false
        isPlaceholderName("The Adicts") shouldBe false
        isPlaceholderName("DJ Shadow") shouldBe false
    }

    // --- isNonArtistLabel ---

    @Test
    fun `isNonArtistLabel returns true for bare role labels`() {
        isNonArtistLabel("Special Guest") shouldBe true
        isNonArtistLabel("Special Guests") shouldBe true
        isNonArtistLabel("Support") shouldBe true
        isNonArtistLabel("div. Supports") shouldBe true
        isNonArtistLabel("SPECIAL GUEST") shouldBe true
        isNonArtistLabel("Support:") shouldBe true
    }

    @Test
    fun `isNonArtistLabel returns false for real names that merely contain a label word`() {
        isNonArtistLabel("Green Lung") shouldBe false
        isNonArtistLabel("Special Guest Stars") shouldBe false
        isNonArtistLabel("") shouldBe false
    }

    // --- isEventSegmentLabel ---

    @Test
    fun `isEventSegmentLabel returns true for aftershow and warm-up segments`() {
        isEventSegmentLabel("ACID AFTERSHOW") shouldBe true
        isEventSegmentLabel("Aftershow") shouldBe true
        isEventSegmentLabel("Aftershow Party") shouldBe true
        isEventSegmentLabel("Techno Afterparty") shouldBe true
        isEventSegmentLabel("Warm Up") shouldBe true
        isEventSegmentLabel("warm-up") shouldBe true
    }

    @Test
    fun `isEventSegmentLabel is fully anchored so real names survive`() {
        // A real band whose name resembles a segment word.
        isEventSegmentLabel("AFTERHOURS") shouldBe false
        // A qualified/named slot that carries more than the bare segment phrase.
        isEventSegmentLabel("Warm Up im Franken") shouldBe false
        isEventSegmentLabel("The Muppet Show") shouldBe false
        isEventSegmentLabel("Green Lung") shouldBe false
    }

    // --- isNonArtistEvent ---

    @Test
    fun `isNonArtistEvent returns true for festival and festival-ticket labels`() {
        isNonArtistEvent("SHRED FEST") shouldBe true
        isNonArtistEvent("GROBES FEST 2026") shouldBe true
        isNonArtistEvent("CANARIAS CALLING FESTIVAL") shouldBe true
        isNonArtistEvent("GROSSSTADTWAHNSINN 2026 - FESTIVALTICKET") shouldBe true
    }

    @Test
    fun `isNonArtistEvent keeps one-word names that merely contain fest`() {
        isNonArtistEvent("Infest") shouldBe false
        isNonArtistEvent("Manifest") shouldBe false
        isNonArtistEvent("Green Lung") shouldBe false
    }

    // --- isNonArtistName curated denylist ---

    @Test
    fun `isNonArtistName drops curated one-off non-artist titles`() {
        isNonArtistName("Warm Up im Franken") shouldBe true
        isNonArtistName("THE REVIVAL TOUR") shouldBe true
        isNonArtistName("Music Quiz") shouldBe true
        isNonArtistName("Open Mic L. J. Fox") shouldBe true
        // Recurring series: any edition number matches.
        isNonArtistName("FEMALE-FRONTED IS NOT A GENRE 5") shouldBe true
        isNonArtistName("FEMALE-FRONTED IS NOT A GENRE 6") shouldBe true
    }

    @Test
    fun `isNonArtistName keeps real names including those ending in a number`() {
        isNonArtistName("WEDNESDAY 13") shouldBe false
        isNonArtistName("OXO86") shouldBe false
        isNonArtistName("The Adicts") shouldBe false
    }

    // --- stripArtistSuffix ---

    @Test
    fun `stripArtistSuffix recovers the act from tour and live suffixes`() {
        stripArtistSuffix("DOMINIUM - NIGHT IS CALLING TOUR 2026") shouldBe "DOMINIUM"
        stripArtistSuffix("AZ LIVE IN BERLIN") shouldBe "AZ"
        stripArtistSuffix("HGICH.T LIVE") shouldBe "HGICH.T"
    }

    @Test
    fun `stripArtistSuffix recovers the act from an anniversary suffix`() {
        stripArtistSuffix("THE BUTLERS - 40 YEARS, SKA & SOULPOWER -") shouldBe "THE BUTLERS"
        stripArtistSuffix("SELIG - 30 JAHRE") shouldBe "SELIG"
    }

    @Test
    fun `stripArtistSuffix strips a parenthesized performance-format annotation`() {
        stripArtistSuffix("Avangelic (DJ-Set)") shouldBe "Avangelic"
        stripArtistSuffix("Someone (DJ Set)") shouldBe "Someone"
        stripArtistSuffix("Band (Acoustic)") shouldBe "Band"
    }

    @Test
    fun `stripArtistSuffix leaves plain names, a bare Live band and a parenthesized alias untouched`() {
        stripArtistSuffix("The Adicts") shouldBe "The Adicts"
        stripArtistSuffix("Live") shouldBe "Live"
        // No tour/anniversary marker in the hyphenated tail, so it is not a suffix.
        stripArtistSuffix("BAD COMPANY LEGACY - Dave Colwell") shouldBe "BAD COMPANY LEGACY - Dave Colwell"
        // The parenthetical is an alias, not a format word, so it is kept.
        stripArtistSuffix("Sickboyrari (Black Kray)") shouldBe "Sickboyrari (Black Kray)"
    }

    // --- buildArtistList ---

    @Test
    fun `buildArtistList returns empty when supportNames is empty`() {
        buildArtistList("Headliner", emptyList()).shouldBeEmpty()
    }

    @Test
    fun `buildArtistList returns headliner and supports`() {
        val result = buildArtistList("The Adicts", listOf("Maid of Ace", "Kaos"))
        result shouldHaveSize 3
        result[0] shouldBe ScrapedArtist(name = "The Adicts", role = "HEADLINER")
        result[1] shouldBe ScrapedArtist(name = "Maid of Ace", role = "SUPPORT")
        result[2] shouldBe ScrapedArtist(name = "Kaos", role = "SUPPORT")
    }

    @Test
    fun `buildArtistList excludes placeholder headliner`() {
        val result = buildArtistList("TBA", listOf("Support Act"))
        result shouldHaveSize 1
        result[0] shouldBe ScrapedArtist(name = "Support Act", role = "SUPPORT")
    }

    @Test
    fun `buildArtistList excludes placeholder support names`() {
        val result = buildArtistList("The Adicts", listOf("TBA", "Maid of Ace"))
        result shouldHaveSize 2
        result[0] shouldBe ScrapedArtist(name = "The Adicts", role = "HEADLINER")
        result[1] shouldBe ScrapedArtist(name = "Maid of Ace", role = "SUPPORT")
    }

    @Test
    fun `buildArtistList with all placeholder supports returns only headliner`() {
        val result = buildArtistList("The Adicts", listOf("TBA", "TBD"))
        result shouldHaveSize 1
        result[0] shouldBe ScrapedArtist(name = "The Adicts", role = "HEADLINER")
    }

    @Test
    fun `buildArtistList drops a bare role-label support but keeps the headliner`() {
        // A "Support: Special Guest" line still signals the title-as-headliner
        // convention, but the label itself must not become a support artist.
        val result = buildArtistList("Green Lung", listOf("Special Guest"))
        result shouldContainExactly listOf(ScrapedArtist(name = "Green Lung", role = "HEADLINER"))
    }

    @Test
    fun `buildArtistList splits a multi-artist title into co-headliners`() {
        val result = buildArtistList("TOTAL CHAOS + RUMKICKS", listOf("The Dollheads"))
        result shouldContainExactly
            listOf(
                ScrapedArtist(name = "TOTAL CHAOS", role = "HEADLINER"),
                ScrapedArtist(name = "RUMKICKS", role = "HEADLINER"),
                ScrapedArtist(name = "The Dollheads", role = "SUPPORT")
            )
    }

    // --- splitHeadlinerTitle ---

    @Test
    fun `splitHeadlinerTitle splits space-padded plus and slash co-bills`() {
        splitHeadlinerTitle("TOTAL CHAOS + RUMKICKS + THE DOLLHEADS") shouldContainExactly
            listOf("TOTAL CHAOS", "RUMKICKS", "THE DOLLHEADS")
        splitHeadlinerTitle("LAGWAGON / THE VIRGINMARYS") shouldContainExactly
            listOf("LAGWAGON", "THE VIRGINMARYS")
    }

    @Test
    fun `splitHeadlinerTitle splits a genuine ampersand co-bill`() {
        splitHeadlinerTitle("BLACK STAR RIDERS & TYKETTO") shouldContainExactly
            listOf("BLACK STAR RIDERS", "TYKETTO")
    }

    @Test
    fun `splitHeadlinerTitle splits guarded and und conjunctions`() {
        splitHeadlinerTitle("Earth Tongue und Scott Hepple") shouldContainExactly
            listOf("Earth Tongue", "Scott Hepple")
        splitHeadlinerTitle("Killswitch Engage and Parkway Drive") shouldContainExactly
            listOf("Killswitch Engage", "Parkway Drive")
    }

    @Test
    fun `splitHeadlinerTitle splits a real co-bill even when another act is an and-the-Ys band`() {
        // Cuts only at the "&"; the " AND THE GREAT BAND" tail stays joined to its act.
        splitHeadlinerTitle("CARL CARLTON & MELANIE WIEGMANN AND THE GREAT BAND") shouldContainExactly
            listOf("CARL CARLTON", "MELANIE WIEGMANN AND THE GREAT BAND")
    }

    @Test
    fun `splitHeadlinerTitle keeps single acts whose name contains a separator`() {
        // No space padding around the slash.
        splitHeadlinerTitle("AC/DC") shouldContainExactly listOf("AC/DC")
        // Denylisted ampersand name.
        splitHeadlinerTitle("Simon & Garfunkel") shouldContainExactly listOf("Simon & Garfunkel")
        // Denylist matches even when the source spells the conjunction as "and".
        splitHeadlinerTitle("Simon and Garfunkel") shouldContainExactly listOf("Simon and Garfunkel")
        // "X & the Ys" band-name tail, in both & and "and" forms.
        splitHeadlinerTitle("Nick Cave & the Bad Seeds") shouldContainExactly listOf("Nick Cave & the Bad Seeds")
        splitHeadlinerTitle("James and the Cold Gun") shouldContainExactly listOf("James and the Cold Gun")
        // A bare "and" inside a single word must not be split (space-padding).
        splitHeadlinerTitle("Portland") shouldContainExactly listOf("Portland")
        // Comma signals a member-list band name.
        splitHeadlinerTitle("Earth, Wind & Fire") shouldContainExactly listOf("Earth, Wind & Fire")
    }

    @Test
    fun `splitHeadlinerTitle returns a singleton for a plain single-act title`() {
        splitHeadlinerTitle("The Adicts") shouldContainExactly listOf("The Adicts")
        splitHeadlinerTitle("  The Adicts  ") shouldContainExactly listOf("The Adicts")
    }

    // --- headlinersFromTitle ---

    @Test
    fun `headlinersFromTitle drops placeholder fragments from a split title`() {
        headlinersFromTitle("TBA + Real Band") shouldContainExactly
            listOf(ScrapedArtist(name = "Real Band", role = "HEADLINER"))
        headlinersFromTitle("TBA").shouldBeEmpty()
    }

    @Test
    fun `headlinersFromTitle strips tour and live suffixes to recover the act`() {
        headlinersFromTitle("DOMINIUM - NIGHT IS CALLING TOUR 2026") shouldContainExactly
            listOf(ScrapedArtist(name = "DOMINIUM", role = "HEADLINER"))
        headlinersFromTitle("HGICH.T LIVE") shouldContainExactly
            listOf(ScrapedArtist(name = "HGICH.T", role = "HEADLINER"))
    }

    @Test
    fun `headlinersFromTitle recovers a single act from an anniversary title`() {
        // The comma in the tail keeps the title unsplit; the suffix strip then recovers the band.
        headlinersFromTitle("THE BUTLERS - 40 YEARS, SKA & SOULPOWER -") shouldContainExactly
            listOf(ScrapedArtist(name = "THE BUTLERS", role = "HEADLINER"))
    }

    @Test
    fun `headlinersFromTitle drops festival and ticket titles`() {
        headlinersFromTitle("SHRED FEST").shouldBeEmpty()
        headlinersFromTitle("GROSSSTADTWAHNSINN 2026 - FESTIVALTICKET").shouldBeEmpty()
    }

    // --- detectFree ---

    @Test
    fun `detectFree is true for an explicit zero presale or box-office price`() {
        detectFree(pricePresale = BigDecimal.ZERO) shouldBe true
        detectFree(pricePresale = BigDecimal("0.00")) shouldBe true
        detectFree(priceBoxOffice = BigDecimal("0.00")) shouldBe true
    }

    @Test
    fun `detectFree is true for free-entry phrases in the price note or title`() {
        detectFree(priceNote = "Eintritt frei") shouldBe true
        detectFree(priceNote = "Freier Eintritt, Spende erwünscht") shouldBe true
        detectFree(priceNote = "Free entry all night") shouldBe true
        detectFree(title = "Sommerfest — Free Admission") shouldBe true
    }

    @Test
    fun `detectFree matches single-word markers only in the price note`() {
        detectFree(priceNote = "Gratis") shouldBe true
        detectFree(priceNote = "kostenlos") shouldBe true
        detectFree(priceNote = "umsonst") shouldBe true
        // A bare token in the title (not the pricing-scoped note) must not trigger.
        detectFree(title = "Gratis Vibes Live") shouldBe false
    }

    @Test
    fun `detectFree does not false-positive on names or word fragments`() {
        detectFree(title = "Freedom Festival") shouldBe false
        detectFree(title = "Freikörperkultur") shouldBe false
        detectFree(priceNote = "freestyle session") shouldBe false
        detectFree(pricePresale = BigDecimal("12.00"), priceBoxOffice = BigDecimal("15.00")) shouldBe false
    }

    @Test
    fun `detectFree is false when nothing is provided`() {
        detectFree() shouldBe false
        detectFree(priceNote = null, title = null) shouldBe false
    }
}
