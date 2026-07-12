package de.norm.events.scraper.clash

import de.norm.events.event.EventType
import de.norm.events.scraper.ScrapedArtist
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [ClashOverviewPageScraper].
 *
 * Parses the saved homepage fixture and asserts the extracted fields, including a
 * fully-populated representative event, title-based type inference (quiz), and the
 * sparse-data edge cases (no subtitle / no ticket link / midnight placeholder time).
 */
class ClashOverviewPageScraperTest {
    private val scraper = ClashOverviewPageScraper()
    private val baseUrl = "https://clash-berlin.de/"

    private fun parseFixture() =
        scraper.scrape(
            Jsoup.parse(
                javaClass.classLoader
                    .getResourceAsStream("scraper/clash/clash-overview.html")!!
                    .bufferedReader()
                    .readText(),
                baseUrl
            ),
            baseUrl
        )

    @Test
    fun `scrape extracts all events from fixture`() {
        parseFixture() shouldHaveSize 5
    }

    @Test
    fun `scrape fully populates a representative ticketed event`() {
        val event = parseFixture().first { it.title == "25 Years Lucha Amada Festival - Day 1" }

        event.subtitle shouldBe "Live: Cheb Balowski / Cuatro Pesos de Propina"
        event.eventDate shouldBe LocalDate.of(2026, 9, 11)
        event.startTime shouldBe LocalTime.of(20, 0)
        event.imageUrl shouldBe "https://clash-berlin.de/wp-content/uploads/2026/02/25luchaamada.jpg"
        event.ticketUrl shouldBe "https://clash.stager.co/shop/tickets/events/111622409"
        event.sourceUrl shouldBe "https://clash-berlin.de/#1109202619407"
        event.sourceId shouldBe "clash:1109202619407"
        event.eventType shouldBe EventType.CONCERT.name
        // Lineup read from the "Live: …" subtitle: first act headlines, the rest support.
        event.artists shouldContainExactly
            listOf(
                ScrapedArtist("Cheb Balowski", "HEADLINER"),
                ScrapedArtist("Cuatro Pesos de Propina", "SUPPORT")
            )
    }

    @Test
    fun `scrape reads the concert lineup from a bare slash-separated subtitle`() {
        val event = parseFixture().first { it.title == "POPPERKLOPPER (New Album Release Show)" }

        // The decorated title is not used as an artist source; the clean subtitle lineup is.
        event.artists shouldContainExactly
            listOf(
                ScrapedArtist("Popperklopper", "HEADLINER"),
                ScrapedArtist("Hausvabot", "SUPPORT"),
                ScrapedArtist("Ad Nauseam", "SUPPORT")
            )
    }

    @Test
    fun `scrape infers quiz type from the title`() {
        val quiz = parseFixture().first { it.title == "Kneipenquiz" }

        quiz.eventType shouldBe EventType.QUIZ.name
        quiz.eventDate shouldBe LocalDate.of(2026, 9, 16)
        quiz.startTime shouldBe LocalTime.of(19, 30)
        quiz.subtitle.shouldBeNull()
        quiz.ticketUrl.shouldBeNull()
        quiz.artists shouldHaveSize 0
    }

    @Test
    fun `scrape does not mint a prose tagline subtitle as an artist`() {
        // A concert whose subtitle is a tagline, not a lineup (no "Live:"/"DJ:" label, no separator).
        val html =
            """
            <div class="gigs-container">
              <div class="item">
                <div class="gig-info"><div class="info-content">
                  <h3 class="gig-title">Hard Skin</h3>
                  <h4 class="sub-title">Last Show Ever in Berlin</h4>
                  <div class="meta"><ul class="meta-list"><li class="time">Thu 20:00</li></ul></div>
                  <div class="collapse infofull" id="0110202600001">
                    <span class="dateTwo">01.10.26</span>
                  </div>
                </div></div>
              </div>
            </div>
            """.trimIndent()

        val event = scraper.scrape(Jsoup.parse(html, baseUrl), baseUrl).single()

        event.subtitle shouldBe "Last Show Ever in Berlin"
        event.artists shouldHaveSize 0
    }

    @Test
    fun `scrape handles a sparse event with a single-digit placeholder time`() {
        val event = parseFixture().first { it.title == "BAU PAUSE" }

        event.eventDate shouldBe LocalDate.of(2026, 6, 29)
        event.startTime shouldBe LocalTime.of(0, 0)
        event.subtitle.shouldBeNull()
        event.ticketUrl.shouldBeNull()
        event.imageUrl shouldBe "https://clash-berlin.de/wp-content/uploads/2026/06/Bauarbeiten-A3.jpg"
    }

    @Test
    fun `scrape returns empty list for a page without events`() {
        val document = Jsoup.parse("<html><body><section id='events'></section></body></html>", baseUrl)

        scraper.scrape(document, baseUrl) shouldHaveSize 0
    }

    @Test
    fun `scrape skips an item missing its title or date`() {
        val html =
            """
            <div class="gigs-container">
              <div class="item">
                <div class="gig-info"><div class="info-content">
                  <div class="collapse infofull" id="0101202700001">
                    <span class="dateTwo">01.01.27</span>
                  </div>
                </div></div>
              </div>
              <div class="item">
                <div class="gig-info"><div class="info-content">
                  <h3 class="gig-title">No Date Here</h3>
                  <div class="collapse infofull" id="0000000000002"></div>
                </div></div>
              </div>
            </div>
            """.trimIndent()

        scraper.scrape(Jsoup.parse(html, baseUrl), baseUrl) shouldHaveSize 0
    }
}
