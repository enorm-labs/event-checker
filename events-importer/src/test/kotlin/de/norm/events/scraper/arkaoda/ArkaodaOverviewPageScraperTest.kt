package de.norm.events.scraper.arkaoda

import de.norm.events.event.EventType
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [ArkaodaOverviewPageScraper].
 *
 * Parses the real `?/default/program` snapshot. The listing only ever shows the
 * venue's *upcoming* events, so the captured page holds two — one `Konser`-labelled
 * concert and one unlabelled night — which is exactly the split the type and artist
 * rules turn on.
 */
class ArkaodaOverviewPageScraperTest {
    private val scraper = ArkaodaOverviewPageScraper()
    private val baseUrl = "https://berlin.arkaoda.com/?/default/program"

    private fun listing() =
        Jsoup.parse(
            javaClass.classLoader
                .getResourceAsStream("scraper/arkaoda/arkaoda-program.html")!!
                .bufferedReader()
                .readText(),
            baseUrl
        )

    private fun events() = scraper.scrape(listing(), baseUrl)

    @Test
    fun `parses every event block on the listing`() {
        events() shouldHaveSize 2
    }

    @Test
    fun `extracts all published fields of a Konser-labelled event`() {
        val signal = events().first { it.sourceId == "arkaoda:1320" }
        signal.title shouldBe "Signal To Noise: Vicente Yáñez, Kėkė Søl, Guro Kverndokk"
        signal.eventDate shouldBe LocalDate.of(2026, 7, 30)
        signal.eventType shouldBe EventType.CONCERT.name
        signal.sourceUrl shouldBe "https://berlin.arkaoda.com/?/default/detail/id=1320"
        signal.imageUrl shouldBe "https://berlin.arkaoda.com/uploads/events/20260718141624.jpeg"
        signal.artists.map { it.name } shouldContainExactly
            listOf("Vicente Yáñez", "Kėkė Søl", "Guro Kverndokk")
        // The venue publishes none of these anywhere in its markup.
        signal.doorsTime shouldBe null
        signal.startTime shouldBe null
        signal.ticketUrl shouldBe null
        signal.genre shouldBe null
        signal.pricePresale shouldBe null
        signal.soldOut shouldBe false
    }

    @Test
    fun `leaves the description to the detail page rather than storing the truncated excerpt`() {
        events().forEach { it.description shouldBe null }
    }

    @Test
    fun `types an unlabelled night from its title and mints no artists for it`() {
        val mnjm = events().first { it.sourceId == "arkaoda:1321" }
        mnjm.title shouldBe "MNJM"
        mnjm.eventDate shouldBe LocalDate.of(2026, 7, 31)
        mnjm.eventType shouldBe EventType.OTHER.name
        mnjm.artists.shouldBeEmpty()
        mnjm.imageUrl shouldBe "https://berlin.arkaoda.com/uploads/events/20260718141743.jpg"
    }

    @Test
    fun `returns no events for a page without event blocks`() {
        scraper
            .scrape(Jsoup.parse("<html><body><div id='posts-list'></div></body></html>", baseUrl), baseUrl)
            .shouldBeEmpty()
    }

    /**
     * Assembles a listing of hand-written blocks in the venue's markup shape. Used only
     * for the guard cases below — every field assertion above runs against the real
     * snapshot instead.
     */
    private fun listingOf(vararg blocks: String) =
        scraper.scrape(
            Jsoup.parse("""<html><body><div id="posts-list"><article>${blocks.joinToString("")}</article></div></body></html>""", baseUrl),
            baseUrl
        )

    private fun block(
        header: String,
        href: String = "?/default/detail/id=99",
        title: String = "Some Event",
        flyer: String = """<a href="uploads/events/x.jpg" class="highslide"><img src="uploads/events/x.jpg"/></a>"""
    ) = """<div class="box cf">$flyer<div class="excerpt">$header""" +
        """<h6><a href="$href" class="heading">$title</a></h6></div></div>"""

    @Test
    fun `skips a block whose date is missing or unparseable`() {
        listingOf(
            block(header = "<b> Donnerstag</b>"),
            block(header = "<b>30 / 07</b>", href = "?/default/detail/id=98")
        ).shouldBeEmpty()
    }

    @Test
    fun `skips a block whose detail link carries no event id`() {
        listingOf(
            block(header = "<b>30 / 07 / 2026</b>", href = "?/default/program"),
            block(header = "<b>30 / 07 / 2026</b>", href = "")
        ).shouldBeEmpty()
    }

    @Test
    fun `skips a block with a blank title`() {
        listingOf(block(header = "<b>30 / 07 / 2026</b>", title = " ")).shouldBeEmpty()
    }

    @Test
    fun `keeps an event that has no flyer`() {
        val events = listingOf(block(header = "<b>30 / 07 / 2026</b>", flyer = ""))
        events shouldHaveSize 1
        events.single().imageUrl shouldBe null
    }
}
