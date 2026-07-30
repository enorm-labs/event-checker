package de.norm.events.scraper.arkaoda

import de.norm.events.event.EventType
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [ArkaodaDetailPageScraper].
 *
 * Each fixture is a real `?/default/detail/id=<n>` snapshot chosen for one behaviour:
 * a comma-separated concert lineup, a `<promoter> pres.` framing with a prose ticket
 * link, a lineup carrying `(PL/USA)` country tags, a title with leaked PHP escapes,
 * an unlabelled club night, and an unpublished id that renders an empty block.
 */
class ArkaodaDetailPageScraperTest {
    private val scraper = ArkaodaDetailPageScraper()

    private fun detailUrl(id: Int) = "https://berlin.arkaoda.com/?/default/detail/id=$id"

    private fun scrape(
        fixture: String,
        id: Int
    ) = scraper.scrape(
        Jsoup.parse(
            javaClass.classLoader
                .getResourceAsStream("scraper/arkaoda/$fixture")!!
                .bufferedReader()
                .readText(),
            detailUrl(id)
        ),
        detailUrl(id)
    )

    @Test
    fun `extracts every field of a Konser-labelled concert`() {
        val event = scrape("arkaoda-detail-concert-lineup.html", 1320).shouldNotBeNull()
        event.title shouldBe "Signal To Noise: Vicente Yáñez, Kėkė Søl, Guro Kverndokk"
        event.eventDate shouldBe LocalDate.of(2026, 7, 30)
        event.eventType shouldBe EventType.CONCERT.name
        event.sourceId shouldBe "arkaoda:1320"
        event.sourceUrl shouldBe detailUrl(1320)
        event.imageUrl shouldBe "https://berlin.arkaoda.com/uploads/events/20260718141624.jpeg"
        event.artists.map { it.name } shouldContainExactly
            listOf("Vicente Yáñez", "Kėkė Søl", "Guro Kverndokk")
        event.promoters.shouldBeEmpty()
    }

    @Test
    fun `keeps the untruncated description and its paragraph breaks`() {
        val description = scrape("arkaoda-detail-concert-lineup.html", 1320).shouldNotBeNull().description
        description.shouldNotBeNull()
        description shouldContain "Signal to Noise is an event series at Arkaoda"
        // Text the listing excerpt cuts off — the reason the detail page is fetched at all.
        description shouldContain "€10 Entry on the door"
        description shouldNotContain "weiterlesen"
        description shouldContain "\n"
    }

    @Test
    fun `reads the promoter and the labelled ticket link out of the prose`() {
        val event = scrape("arkaoda-detail-presents-prefix.html", 1290).shouldNotBeNull()
        event.title shouldBe "pre:sense pres. Volpe (Live)"
        event.eventDate shouldBe LocalDate.of(2026, 6, 11)
        event.promoters shouldContainExactly listOf("pre:sense")
        event.artists.map { it.name } shouldContainExactly listOf("Volpe")
        event.ticketUrl shouldBe "https://ra.co/events/2448980"
        // Prices are left to the description even when it names one ("Limited Tickets at Door").
        event.priceBoxOffice shouldBe null
        event.pricePresale shouldBe null
    }

    @Test
    fun `splits a lineup without fragmenting its country tags`() {
        val event = scrape("arkaoda-detail-slash-lineup.html", 1289).shouldNotBeNull()
        event.eventDate shouldBe LocalDate.of(2026, 6, 24)
        event.artists.map { it.name } shouldContainExactly
            listOf("Chris Pitsiokos", "Qba Janicki", "Marta Warelis", "IFS meets AGF")
    }

    @Test
    fun `unescapes the leaked PHP escapes in a title and mints no artists from a release party`() {
        val event = scrape("arkaoda-detail-escaped-quotes.html", 1293).shouldNotBeNull()
        event.title shouldBe """Post Clients & Friends: 7" Vinyl Release Party"""
        event.eventDate shouldBe LocalDate.of(2026, 6, 3)
        event.eventType shouldBe EventType.CONCERT.name
        event.artists.shouldBeEmpty()
        // No "Tickets:" label in this description, so no link is guessed at.
        event.ticketUrl shouldBe null
    }

    @Test
    fun `types an unlabelled night from its title and mints no artists`() {
        val event = scrape("arkaoda-detail-party-untyped.html", 1321).shouldNotBeNull()
        event.title shouldBe "MNJM"
        event.eventType shouldBe EventType.OTHER.name
        event.artists.shouldBeEmpty()
        event.description.shouldNotBeNull() shouldContain "tickets at the door"
        // "tickets at the door" is a label without a link — nothing to store.
        event.ticketUrl shouldBe null
    }

    @Test
    fun `returns null for an unpublished id that renders an empty block`() {
        scrape("arkaoda-detail-empty.html", 1322) shouldBe null
    }

    @Test
    fun `returns null for a page without an event block`() {
        val url = detailUrl(1320)
        scraper.scrape(Jsoup.parse("<html><body></body></html>", url), url) shouldBe null
    }

    @Test
    fun `returns null when the URL carries no event id to key the sourceId on`() {
        val url = "https://berlin.arkaoda.com/?/default/detail"
        val page =
            """<html><body><div id="posts-list"><div class="box"><div class="excerpt">""" +
                """<b>30 / 07 / 2026</b><h6 class="heading">Some Event</h6></div></div></div></body></html>"""
        scraper.scrape(Jsoup.parse(page, url), url) shouldBe null
    }

    @Test
    fun `keeps an event whose page has no sidebar flyer or body text`() {
        val url = detailUrl(1320)
        val page =
            """<html><body><div id="posts-list"><div class="box"><div class="excerpt">""" +
                """<b>30 / 07 / 2026</b><h6 class="heading">Some Event</h6></div></div></div></body></html>"""
        val event = scraper.scrape(Jsoup.parse(page, url), url).shouldNotBeNull()
        event.imageUrl shouldBe null
        event.description shouldBe null
        event.ticketUrl shouldBe null
    }
}
