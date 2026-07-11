package de.norm.events.scraper

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalTime

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

    @Test
    fun `mapEventType maps a public-viewing label to SCREENING`() {
        mapEventType("Public Viewing") shouldBe "SCREENING"
    }

    // --- inferConcertVenueType ---

    @Test
    fun `inferConcertVenueType defaults a plain artist title to CONCERT`() {
        // Band names a concert-venue left uncategorised (Astra Green Lung, So36 NOFX,
        // Lido Pomplamoose/Dea Matrona/South Arcade) — the title names the act.
        inferConcertVenueType("GREEN LUNG") shouldBe "CONCERT"
        inferConcertVenueType("NOFX - 40 YEARS OF FUCKING UP") shouldBe "CONCERT"
        inferConcertVenueType("POMPLAMOOSE") shouldBe "CONCERT"
    }

    @Test
    fun `inferConcertVenueType detects a quiz`() {
        inferConcertVenueType("Quiz Night Show") shouldBe "QUIZ"
    }

    @Test
    fun `inferConcertVenueType detects a wrestling or burlesque show`() {
        inferConcertVenueType("QUEER WRESTLING CIRCUS") shouldBe "SHOW"
        inferConcertVenueType("BERLIN FREAK BURLESQUE CIRCUS") shouldBe "SHOW"
    }

    @Test
    fun `inferConcertVenueType maps football and cinema formats to SCREENING`() {
        inferConcertVenueType("11FREUNDE WM-QUARTIER") shouldBe "SCREENING"
        inferConcertVenueType("Fußball Weltmeisterschaft") shouldBe "SCREENING"
        inferConcertVenueType("World Cup 2026 Live Screening") shouldBe "SCREENING"
        inferConcertVenueType("KENNEN SIE KINO?") shouldBe "SCREENING"
    }

    @Test
    fun `inferConcertVenueType maps other non-music formats to OTHER`() {
        inferConcertVenueType("DAV JURA SLAM") shouldBe "OTHER"
        inferConcertVenueType("LESEDÜNE") shouldBe "OTHER"
    }

    @Test
    fun `inferConcertVenueType keeps an act whose name merely contains kino as a substring`() {
        // "kino" is only a whole-word cinema marker, so "AlKINOos" stays a CONCERT.
        inferConcertVenueType("ALKINOOS IOANNIDIS") shouldBe "CONCERT"
    }

    // --- isScreeningTitle ---

    @Test
    fun `isScreeningTitle detects football public-viewings and cinema nights`() {
        isScreeningTitle("11FREUNDE WM-QUARTIER") shouldBe true
        isScreeningTitle("World Cup 2026 Live Screening") shouldBe true
        isScreeningTitle("Fußball Weltmeisterschaft") shouldBe true
        isScreeningTitle("KENNEN SIE KINO?") shouldBe true
        isScreeningTitle("SHORTIES FILMS SCREENING #28") shouldBe true
    }

    @Test
    fun `isScreeningTitle keeps a plain act, including a kino substring`() {
        isScreeningTitle("GREEN LUNG") shouldBe false
        isScreeningTitle("ALKINOOS IOANNIDIS") shouldBe false
    }

    @Test
    fun `inferConcertVenueType detects a party or club night`() {
        inferConcertVenueType("THE CURE AFTERSHOW PARTY") shouldBe "PARTY"
        inferConcertVenueType("P ▲ R ▲ N ● I ► (PARANOID CLUB)") shouldBe "PARTY"
    }

    // --- refineConcertVenueType ---

    @Test
    fun `refineConcertVenueType defaults an unclassified event to CONCERT`() {
        // No category (null) → the title names the act at a live-music venue.
        refineConcertVenueType(null, "GREEN LUNG") shouldBe "CONCERT"
    }

    @Test
    fun `refineConcertVenueType trusts a specific venue category`() {
        refineConcertVenueType("PARTY", "Some DJ Night") shouldBe "PARTY"
        refineConcertVenueType("FESTIVAL", "Some Weekender") shouldBe "FESTIVAL"
    }

    @Test
    fun `refineConcertVenueType reclassifies a generic OTHER only on a keyword`() {
        // Astra tags its wrestling show with the generic "Other" kind (→ OTHER); a
        // keyword recovers it, but a signal-less catch-all stays OTHER rather than
        // being force-promoted to CONCERT.
        refineConcertVenueType("OTHER", "QUEER WRESTLING CIRCUS") shouldBe "SHOW"
        refineConcertVenueType("OTHER", "11FREUNDE WM-QUARTIER") shouldBe "SCREENING"
        refineConcertVenueType("OTHER", "GWF Summer Smash 2026") shouldBe "OTHER"
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
    fun `isNonArtistEvent returns true for a festival slot or edition with trailing text`() {
        isNonArtistEvent("Grey City Fest Opener") shouldBe true
        isNonArtistEvent("Sommer Festival Special") shouldBe true
    }

    @Test
    fun `isNonArtistEvent keeps one-word names and compounds that merely contain fest`() {
        isNonArtistEvent("Infest") shouldBe false
        isNonArtistEvent("Manifest") shouldBe false
        isNonArtistEvent("Sommerfest") shouldBe false
        isNonArtistEvent("Green Lung") shouldBe false
    }

    // --- isFestivalTitle ---

    @Test
    fun `isFestivalTitle matches word-anchored festival and festival-ticket markers`() {
        isFestivalTitle("CANARIAS CALLING FESTIVAL") shouldBe true
        isFestivalTitle("TANGO OR NONTANGO FESTIVAL") shouldBe true
        isFestivalTitle("GROSSSTADTWAHNSINN 2026 - FESTIVALTICKET") shouldBe true
    }

    @Test
    fun `isFestivalTitle ignores a bare fest and plain titles`() {
        // Tighter than isNonArtistEvent: a bare "Fest" is too weak a signal to retype an event.
        isFestivalTitle("GROBES FEST 2026") shouldBe false
        isFestivalTitle("Manifest") shouldBe false
        isFestivalTitle("Berliner Weisse") shouldBe false
    }

    // --- cleanEventTitle ---

    @Test
    fun `cleanEventTitle strips a trailing reschedule note and stray dash`() {
        cleanEventTitle("Iggi Kelly Nachholtermin vom 28.04.26-") shouldBe "Iggi Kelly"
        cleanEventTitle("The Dear Hunter -Nachholtermin vom 30.09.2025.") shouldBe "The Dear Hunter"
        cleanEventTitle("Some Show -") shouldBe "Some Show"
    }

    @Test
    fun `cleanEventTitle strips a trailing sold-out annotation`() {
        // The status suffix is dropped so "… (ausverkauft)" and its non-sold-out twin
        // collapse to the same title (and the same title-derived headliner artist).
        cleanEventTitle("Singalong -Das große Mitsing-Event (ausverkauft)") shouldBe "Singalong -Das große Mitsing-Event"
        cleanEventTitle("Singalong -Das große Mitsing-Event") shouldBe "Singalong -Das große Mitsing-Event"
        cleanEventTitle("Some Show ausverkauft") shouldBe "Some Show"
        cleanEventTitle("Some Show - AUSVERKAUFT!") shouldBe "Some Show"
    }

    @Test
    fun `cleanEventTitle leaves a clean title and mid-title dash untouched`() {
        cleanEventTitle("Freshlyground") shouldBe "Freshlyground"
        cleanEventTitle("Tannz im Frannz -auf 2 Floors") shouldBe "Tannz im Frannz -auf 2 Floors"
        // "ausverkauft" only mid-title (never a real case, but proves the end-anchor) is kept.
        cleanEventTitle("Ausverkauft Tour Show") shouldBe "Ausverkauft Tour Show"
    }

    // --- isNonArtistName curated denylist ---

    @Test
    fun `isNonArtistName drops curated one-off non-artist titles`() {
        isNonArtistName("Warm Up im Franken") shouldBe true
        isNonArtistName("THE REVIVAL TOUR") shouldBe true
        isNonArtistName("Music Quiz") shouldBe true
        isNonArtistName("Open Mic L. J. Fox") shouldBe true
        isNonArtistName("Feinster HipHop") shouldBe true
        isNonArtistName("Karrera Klub") shouldBe true
        isNonArtistName("The Swag Jam") shouldBe true
        // Bi Nuu party/DJ series its structured `performers` list names as the act.
        isNonArtistName("GrooveJet Berlin") shouldBe true
        isNonArtistName("Ultra Night") shouldBe true
        // Recurring series: any edition number matches — both the plain and the N°<n> form.
        isNonArtistName("FEMALE-FRONTED IS NOT A GENRE 5") shouldBe true
        isNonArtistName("FEMALE-FRONTED IS NOT A GENRE 6") shouldBe true
        isNonArtistName("Boheme Sauvage N°141") shouldBe true
        isNonArtistName("Boheme Sauvage N°142") shouldBe true
    }

    @Test
    fun `isNonArtistName keeps real names including those ending in a number`() {
        isNonArtistName("WEDNESDAY 13") shouldBe false
        isNonArtistName("OXO86") shouldBe false
        isNonArtistName("The Adicts") shouldBe false
    }

    @Test
    fun `isNonArtistName normalizes accents and a trailing Berlin before denylist matching`() {
        // Accented, city-suffixed editions of the same series fold onto one entry ("boheme sauvage").
        isNonArtistName("Bohème Sauvage Berlin") shouldBe true
        isNonArtistName("BOHÈME SAUVAGE BERLIN") shouldBe true
        // The city suffix is likewise stripped for GrooveJet (entry is city-free "groovejet").
        isNonArtistName("GrooveJet Berlin") shouldBe true
        // Matching-only: a real act merely ending in "Berlin" loses the suffix too but isn't denylisted.
        isNonArtistName("Isolation Berlin") shouldBe false
    }

    // --- isDjSetFormatLabel ---

    @Test
    fun `isDjSetFormatLabel drops a bare DJ-set format label, with or without a slash-origin tail`() {
        isDjSetFormatLabel("DJ-Set") shouldBe true
        isDjSetFormatLabel("DJ Set") shouldBe true
        isDjSetFormatLabel("DJ-Set / Berlin") shouldBe true
        isDjSetFormatLabel("dj-set / london, uk") shouldBe true
        isNonArtistName("DJ-Set / Berlin") shouldBe true
    }

    @Test
    fun `isDjSetFormatLabel keeps a real DJ act whose name only starts with DJ Set`() {
        // Anchored: a name that merely starts with the label, or any "DJ <handle>", survives.
        isDjSetFormatLabel("DJ Koze") shouldBe false
        isDjSetFormatLabel("DJ Set Sail") shouldBe false
        isNonArtistName("DJ Koze") shouldBe false
    }

    // --- splitSegmentOnConjunctions ---

    @Test
    fun `splitSegmentOnConjunctions splits guarded conjunctions but never a slash`() {
        splitSegmentOnConjunctions("Lichene & Neue K") shouldBe listOf("Lichene", "Neue K")
        // A "/" inside a single act name is preserved (not a co-bill separator here).
        splitSegmentOnConjunctions("Morimoto / Wong duo") shouldBe listOf("Morimoto / Wong duo")
        // Backing-band article tail stays joined.
        splitSegmentOnConjunctions("Scott Hepple & The Sun Band") shouldBe listOf("Scott Hepple & The Sun Band")
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
    fun `stripArtistSuffix strips a bare non-parenthesized DJ-Set tail`() {
        stripArtistSuffix("Acid Arab DJ-Set") shouldBe "Acid Arab"
        stripArtistSuffix("Paty Vapor DJ Set") shouldBe "Paty Vapor"
        // A bare "DJ-Set" with no preceding name is left for the non-artist filter to drop.
        stripArtistSuffix("DJ-Set") shouldBe "DJ-Set"
        // "DJ <handle>" acts are not a DJ-Set tail and stay intact.
        stripArtistSuffix("DJ Koze") shouldBe "DJ Koze"
    }

    @Test
    fun `stripArtistSuffix strips a German Nachholtermin rescheduled-date tail`() {
        // With a leading dash directly on the marker (Frannz), with a space-dash (Astra) …
        stripArtistSuffix("The Dear Hunter -Nachholtermin vom 30.09.2025.") shouldBe "The Dear Hunter"
        stripArtistSuffix("Pohlmann -Nachholtermin vom 10.01.-") shouldBe "Pohlmann"
        // … and with no dash at all (Astra).
        stripArtistSuffix("Iggi Kelly Nachholtermin vom 28.04.26-") shouldBe "Iggi Kelly"
    }

    @Test
    fun `stripArtistSuffix strips a German Hochverlegung relocation tail`() {
        // en-dash (Frannz) — the parenthetical alias before the note is preserved.
        stripArtistSuffix("OCT (On Company Time) – Hochverlegung") shouldBe "OCT (On Company Time)"
        stripArtistSuffix("Some Act Hochverlegung") shouldBe "Some Act"
    }

    @Test
    fun `stripArtistSuffix strips a singt tribute framing and a release promo tag`() {
        stripArtistSuffix("Tex singt Leoanard Cohen") shouldBe "Tex"
        stripArtistSuffix("Max Raabe singt Weihnachtslieder") shouldBe "Max Raabe"
        stripArtistSuffix("Hawt Coco Album Release") shouldBe "Hawt Coco"
        stripArtistSuffix("Some Band EP Release Show") shouldBe "Some Band"
        stripArtistSuffix("Some Band Release Party") shouldBe "Some Band"
    }

    @Test
    fun `stripArtistSuffix leaves plain names, a bare Live band and a parenthesized alias untouched`() {
        stripArtistSuffix("The Adicts") shouldBe "The Adicts"
        stripArtistSuffix("Live") shouldBe "Live"
        // No tour/anniversary marker in the hyphenated tail, so it is not a suffix.
        stripArtistSuffix("BAD COMPANY LEGACY - Dave Colwell") shouldBe "BAD COMPANY LEGACY - Dave Colwell"
        // The parenthetical is an alias, not a format word, so it is kept.
        stripArtistSuffix("Sickboyrari (Black Kray)") shouldBe "Sickboyrari (Black Kray)"
        // "Release" without a format word / Party·Show tail is a plausible band name — kept.
        stripArtistSuffix("Release") shouldBe "Release"
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
        // "& Friends" / "& Guests" collective tail names an unnamed cast, not a second act.
        splitHeadlinerTitle("Taylor & Friends") shouldContainExactly listOf("Taylor & Friends")
        splitHeadlinerTitle("Jonny & Guests") shouldContainExactly listOf("Jonny & Guests")
        // A real co-bill alongside a collective tail still splits at the real boundary.
        splitHeadlinerTitle("Ann & the Band + Real Act") shouldContainExactly listOf("Ann & the Band", "Real Act")
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
    fun `headlinersFromTitle strips an event-framing prefix to recover the act`() {
        headlinersFromTitle("A night with GULVØSS II") shouldContainExactly
            listOf(ScrapedArtist(name = "GULVØSS II", role = "HEADLINER"))
        headlinersFromTitle("An Evening with Nick Cave") shouldContainExactly
            listOf(ScrapedArtist(name = "Nick Cave", role = "HEADLINER"))
        // The framing phrase must be a leading whole prefix — a band with "night" mid-name is untouched.
        headlinersFromTitle("Last Night With You") shouldContainExactly
            listOf(ScrapedArtist(name = "Last Night With You", role = "HEADLINER"))
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
        headlinersFromTitle("Grey City Fest Opener").shouldBeEmpty()
        headlinersFromTitle("GROSSSTADTWAHNSINN 2026 - FESTIVALTICKET").shouldBeEmpty()
    }

    @Test
    fun `headlinersFromTitle strips a recurring-series prefix and keeps the billed acts`() {
        // The series label ("OFF THE RAILS #5:") is dropped; the acts after the colon remain.
        headlinersFromTitle("OFF THE RAILS #5: Blake Harley & Superior Motive") shouldContainExactly
            listOf(
                ScrapedArtist(name = "Blake Harley", role = "HEADLINER"),
                ScrapedArtist(name = "Superior Motive", role = "HEADLINER")
            )
    }

    @Test
    fun `headlinersFromTitle keeps a name with a colon but no series edition marker`() {
        // No "#<n>:" marker, so nothing is stripped (guards a real "9:3"-style name).
        headlinersFromTitle("Bleech 9:3") shouldContainExactly
            listOf(ScrapedArtist(name = "Bleech 9:3", role = "HEADLINER"))
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

    // --- orderDoorsBeforeStart ---

    @Test
    fun `orderDoorsBeforeStart swaps a transposed doors-after-start pair`() {
        // SO36's "Einlass: 19:30, Beginn: 19:00" — labels swapped at the source.
        orderDoorsBeforeStart(LocalTime.of(19, 30), LocalTime.of(19, 0)) shouldBe
            (LocalTime.of(19, 0) to LocalTime.of(19, 30))
    }

    @Test
    fun `orderDoorsBeforeStart leaves an already-valid pair unchanged`() {
        orderDoorsBeforeStart(LocalTime.of(19, 0), LocalTime.of(20, 0)) shouldBe
            (LocalTime.of(19, 0) to LocalTime.of(20, 0))
    }

    @Test
    fun `orderDoorsBeforeStart leaves equal times unchanged`() {
        orderDoorsBeforeStart(LocalTime.of(20, 0), LocalTime.of(20, 0)) shouldBe
            (LocalTime.of(20, 0) to LocalTime.of(20, 0))
    }

    @Test
    fun `orderDoorsBeforeStart does not reorder when a time is missing`() {
        orderDoorsBeforeStart(null, LocalTime.of(20, 0)) shouldBe (null to LocalTime.of(20, 0))
        orderDoorsBeforeStart(LocalTime.of(19, 0), null) shouldBe (LocalTime.of(19, 0) to null)
        orderDoorsBeforeStart(null, null) shouldBe (null to null)
    }
}
