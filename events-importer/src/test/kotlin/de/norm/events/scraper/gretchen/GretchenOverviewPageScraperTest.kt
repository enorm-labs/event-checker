package de.norm.events.scraper.gretchen

import de.norm.events.scraper.ScrapedArtist
import de.norm.events.scraper.ScrapedEvent
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [GretchenOverviewPageScraper].
 *
 * Uses a saved snapshot of the real Gretchen homepage as a regression fixture.
 * Gretchen renders full four-digit-year dates, so no clock injection is needed.
 */
class GretchenOverviewPageScraperTest {
    private val baseUrl = "https://www.gretchen-club.de/"
    private val scraper = GretchenOverviewPageScraper()
    private lateinit var html: String

    @BeforeEach
    fun setUp() {
        html =
            javaClass.classLoader
                .getResourceAsStream("scraper/gretchen/gretchen-overview.html")!!
                .bufferedReader()
                .readText()
    }

    private fun scrape(): List<ScrapedEvent> = scraper.scrape(Jsoup.parse(html, baseUrl), baseUrl)

    private fun eventWithId(id: String) = scrape().first { it.sourceId == "gretchen:$id" }

    @Test
    fun `scrape extracts every gig block from the fixture`() {
        scrape() shouldHaveSize 92
    }

    @Nested
    inner class ConcertParsing {
        @Test
        fun `parses a fully populated concert`() {
            val event = eventWithId("3492")

            event.title shouldBe "TULIPA RUIZ"
            event.genre shouldBe "MPB, Pop, Electronica"
            event.eventDate shouldBe LocalDate.of(2026, 7, 10)
            event.doorsTime shouldBe LocalTime.of(19, 30)
            event.startTime shouldBe LocalTime.of(20, 30)
            event.sourceUrl shouldBe "https://www.gretchen-club.de/detail.php?id=3492"
            event.ticketUrl shouldBe "https://ra.co/events/2429097"
            event.status shouldBe "SCHEDULED"
            event.free shouldBe false
        }

        @Test
        fun `extracts the lineup performer as headliner`() {
            eventWithId("3492").artists shouldContainExactly listOf(ScrapedArtist("Tulipa Ruiz", "HEADLINER"))
        }

        @Test
        fun `parses tiered presale and box-office prices`() {
            val event = eventWithId("3492")

            event.pricePresale shouldBe BigDecimal("12")
            event.priceBoxOffice shouldBe BigDecimal("30")
            event.priceNote shouldContain "Vorverkauf 12"
            event.priceNote shouldContain "Abendkasse 30"
        }

        @Test
        fun `resolves and percent-encodes the poster image URL`() {
            val imageUrl = eventWithId("3492").imageUrl!!

            imageUrl shouldContain "https://www.gretchen-club.de/bilder_upload/69ef32032c99b"
            imageUrl shouldContain "%20" // filename contains spaces
        }

        @Test
        fun `drops the venue itself as a promoter`() {
            // "Veranstalter*in: Gretchen" is the venue organising its own night, not an external promoter.
            eventWithId("3492").promoters.shouldBeEmpty()
        }
    }

    @Nested
    inner class ClubNightParsing {
        @Test
        fun `parses a multi-stage lineup in billing order`() {
            val event = eventWithId("3536")

            // Party-style title is kept verbatim; performers come from the .lineup stages, deduplicated.
            event.title shouldBe "saHHara x UNDERGA33 present: DAZE"
            event.artists shouldContainExactly
                listOf(
                    ScrapedArtist("Gnawa Vibes", "HEADLINER"),
                    ScrapedArtist("HearThug", "SUPPORT"),
                    ScrapedArtist("saHHar", "SUPPORT"),
                    ScrapedArtist("Rabibti áTable", "SUPPORT"),
                    ScrapedArtist("Aalia", "SUPPORT"),
                    ScrapedArtist("DINA", "SUPPORT"),
                    ScrapedArtist("Mefteh", "SUPPORT")
                )
        }

        @Test
        fun `keeps an external promoter and omits a missing show time`() {
            val event = eventWithId("3536")

            event.promoters shouldContainExactly listOf("saHHara")
            event.doorsTime shouldBe LocalTime.of(23, 59)
            event.startTime.shouldBeNull()
        }

        @Test
        fun `leaves box-office price null when the source says tba`() {
            val event = eventWithId("3536")

            event.pricePresale shouldBe BigDecimal("10")
            event.priceBoxOffice.shouldBeNull()
        }
    }

    @Nested
    inner class LineupCleaning {
        @Test
        fun `strips bold floor headers instead of fusing them into the first act`() {
            // BOX1 "<b>AFRO FLOOR</b>MaVert", BOX2 "<b>LATIN FLOOR</b>DJ Ebbsolute"; "+ special guests" dropped.
            eventWithId("3537").artists shouldContainExactly
                listOf(
                    ScrapedArtist("MaVert", "HEADLINER"),
                    ScrapedArtist("Tommy Tea", "SUPPORT"),
                    ScrapedArtist("DJ Ebbsolute", "SUPPORT"),
                    ScrapedArtist("Juliany LSP", "SUPPORT"),
                    ScrapedArtist("Juan Virviescas", "SUPPORT")
                )
        }

        @Test
        fun `drops host and visuals credits and member lists, keeps the opening DJ`() {
            // Trailing lineup mixes "Hosted by …", a "(Bass)/(Keys)/(Drums)" member list, and "Opening DJ-Set by …".
            eventWithId("3531").artists shouldContainExactly
                listOf(
                    ScrapedArtist("Peter Somuah", "HEADLINER"),
                    ScrapedArtist("Skyline Sun", "SUPPORT"),
                    ScrapedArtist("Outta Line", "SUPPORT"),
                    ScrapedArtist("Allynx b2b Sean Steinfeger", "SUPPORT")
                )
        }

        @Test
        fun `strips a support prefix and drops the rescheduled-date note`() {
            // Trailing lineup: "Support: Steinza" + "Ersatztermin vom 11.02.2026".
            eventWithId("3450").artists shouldContainExactly
                listOf(
                    ScrapedArtist("Matt Maeson", "HEADLINER"),
                    ScrapedArtist("Steinza", "SUPPORT")
                )
        }

        @Test
        fun `does not mint credit lines, floor headers or notes as artists`() {
            val allArtists = scrape().flatMap { it.artists }.map { it.name }
            allArtists.forEach { name ->
                name shouldNotContain "FLOOR"
                name shouldNotContain "Ersatztermin"
                name shouldNotContain "Hosted by"
                name shouldNotContain "Live Visuals"
                name shouldNotContain "Opening DJ-Set"
            }
        }
    }

    @Nested
    inner class StatusParsing {
        @Test
        fun `maps a cancelled event and strips the status suffix from the title`() {
            val event = eventWithId("3528")

            event.title shouldBe "Deputamadre Club presents: 3BALLMTY"
            event.title shouldNotContain "//"
            event.status shouldBe "CANCELLED"
        }

        @Test
        fun `maps a relocated event and drops the prose relocation note from the lineup`() {
            val event = eventWithId("3420")

            event.title shouldBe "Trinity presents: MUCCO"
            event.status shouldBe "RELOCATED"
            // The .lineup carries a 10-word "verlegt" sentence that must not be minted as an artist.
            event.artists shouldContainExactly listOf(ScrapedArtist("Mucco", "HEADLINER"))
        }
    }

    @Test
    fun `every scraped event has the required identity fields`() {
        scrape().forEach { event ->
            event.title.isNotBlank() shouldBe true
            event.sourceId.startsWith("gretchen:") shouldBe true
            event.sourceUrl.startsWith("https://www.gretchen-club.de/detail.php?id=") shouldBe true
        }
    }
}
