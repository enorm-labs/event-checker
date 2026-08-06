package de.norm.events.scraper.gaertenderwelt

import de.norm.events.event.EventType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [GaertenDerWeltOverviewPageScraper], parsing saved snapshots of the park's
 * `/events/veranstaltungen/` listing (captured 6 August 2026).
 *
 * The first-page snapshot is the breadth filter's regression guard: of its five rows, four are
 * guided tours and workshops and only the games night is programme.
 */
class GaertenDerWeltOverviewPageScraperTest {
    private val scraper = GaertenDerWeltOverviewPageScraper()

    private fun fixture(name: String): Document =
        Jsoup.parse(
            javaClass.classLoader
                .getResourceAsStream("scraper/gaertenderwelt/$name")!!
                .bufferedReader()
                .readText(),
            LISTING_URL
        )

    @Test
    fun `keeps only the staged programme on a listing page`() {
        val events = scraper.scrape(fixture("gaertenderwelt-overview.html"), LISTING_URL)

        // Five rows: two Führungen, two Workshops, and the uncategorised games night.
        events shouldHaveSize 1
        events.single().title shouldBe "Spieleabend"
    }

    @Test
    fun `parses a concert row in full`() {
        val events = scraper.scrape(fixture("gaertenderwelt-overview-page2.html"), LISTING_URL)
        val concert = events.single { it.title == "Agnes Obel" }

        concert.eventType shouldBe EventType.CONCERT.name
        concert.eventDate shouldBe LocalDate.of(2026, 8, 15)
        concert.startTime shouldBe LocalTime.of(19, 0)
        concert.sourceUrl shouldBe "$LISTING_URL/detail/2026-08-15_1900/agnes-obel/"
        concert.sourceId shouldBe "gaerten_der_welt:2026-08-15_1900/agnes-obel"
        concert.imageUrl shouldBe
            "https://www.gaertenderwelt.de/fileadmin/_processed_/f/0/csm_gdw_events_AgnesObel_2025__Alex_Bruel_Flagstad___1__a0332c56eb.png"
        concert.ticketUrl?.startsWith("https://www.eventim.de/event/agnes-obel-gaerten-der-welt-21055531/") shouldBe true
        concert.soldOut shouldBe false
    }

    @Test
    fun `types the open-air cinema from the park's category rather than the title`() {
        val events = scraper.scrape(fixture("gaertenderwelt-overview-page2.html"), LISTING_URL)
        val screening = events.single { it.title.startsWith("Wanderkino") }

        screening.eventType shouldBe EventType.SCREENING.name
        screening.eventDate shouldBe LocalDate.of(2026, 8, 20)
        screening.startTime shouldBe LocalTime.of(21, 0)
        // The park sells no ticket for this one.
        screening.ticketUrl.shouldBeNull()
    }

    @Test
    fun `infers a type from the title when the park files a row under no category`() {
        val gamesNight = scraper.scrape(fixture("gaertenderwelt-overview.html"), LISTING_URL).single()

        gamesNight.eventType shouldBe EventType.OTHER.name
        gamesNight.eventDate shouldBe LocalDate.of(2026, 8, 14)
        gamesNight.startTime shouldBe LocalTime.of(17, 30)
        gamesNight.subtitle shouldBe "Jeden 2. Freitag im Monat"
    }

    @Test
    fun `stores the row teaser as the subtitle`() {
        val events = scraper.scrape(fixture("gaertenderwelt-overview-last.html"), LISTING_URL)

        events.single().subtitle shouldBe "Treibende Beats, legendäre Hits und euphorische Partystimmung!"
    }

    @Test
    fun `follows the paginator's next link`() {
        val page1 = fixture("gaertenderwelt-overview.html")

        scraper.nextPageUrl(page1, LISTING_URL) shouldBe "https://www.gaertenderwelt.de/events/veranstaltungen/page2/"
    }

    @Test
    fun `reports no next page on the last one`() {
        val last = fixture("gaertenderwelt-overview-last.html")

        scraper.nextPageUrl(last, LISTING_URL).shouldBeNull()
        last.select(".paginationWrapper").isEmpty() shouldBe false
    }

    @Test
    fun `returns no events for a listing with no rows`() {
        val empty = Jsoup.parse("""<html><body><div class="tx-events2"><div class="list"></div></div></body></html>""", LISTING_URL)

        scraper.scrape(empty, LISTING_URL) shouldHaveSize 0
        scraper.nextPageUrl(empty, LISTING_URL).shouldBeNull()
    }

    @Test
    fun `skips a row whose detail link carries no date stamp`() {
        val unstamped =
            Jsoup.parse(
                """
                <html><body><div class="tx-events2"><div class="list">
                  <div class="eventWrapper">
                    <div class="category">Konzerte</div>
                    <div class="eventInner">
                      <h3 class="media-heading"><a href="/events/veranstaltungen/detail/some-band/">Some Band</a></h3>
                    </div>
                  </div>
                </div></div></body></html>
                """.trimIndent(),
                LISTING_URL
            )

        scraper.scrape(unstamped, LISTING_URL) shouldHaveSize 0
    }

    private companion object {
        private const val LISTING_URL = "https://www.gaertenderwelt.de/events/veranstaltungen"
    }
}
