package de.norm.events.scraper.roadrunner

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Unit tests for [RoadrunnerOverviewPageScraper].
 *
 * Uses a fixed clock (2026-07-06) so weekday-based year inference is deterministic,
 * and parses the real retro `programm.html` snapshot plus a synthetic multi-event
 * fragment to exercise the dotted-separator splitting.
 */
class RoadrunnerOverviewPageScraperTest {
    private val baseUrl = "http://www.roadrunners-paradise.de/programm.html"

    // The real snapshot was taken while today was 2026-07-06; pin the clock so the
    // year inferred from "Freitag, 29. Mai" is deterministic (29 May 2026 = Friday).
    private val clock: Clock = Clock.fixed(Instant.parse("2026-07-06T10:00:00Z"), ZoneOffset.UTC)
    private val scraper = RoadrunnerOverviewPageScraper(clock)

    private fun programme() =
        Jsoup.parse(
            javaClass.classLoader
                .getResourceAsStream("scraper/roadrunner/roadrunner-programm.html")!!
                .bufferedReader()
                .readText(),
            baseUrl
        )

    @Test
    fun `parses the single dated event from the retro programme page`() {
        val events = scraper.scrape(programme(), baseUrl)
        events shouldHaveSize 1

        val event = events.single()
        event.title shouldBe "BERLIN FREAK BURLESQUE CIRCUS"
        // "Freitag, 29. Mai" with no year → 2026 (the year 29 May falls on a Friday nearest today).
        event.eventDate shouldBe LocalDate.of(2026, 5, 29)
        event.doorsTime shouldBe LocalTime.of(20, 0)
        event.ticketUrl.shouldNotBeNull()
        event.ticketUrl shouldStartWith "https://www.eventbrite.de/e/berlin-freak-burlesque-circus"
        event.imageUrl shouldBe
            "http://www.roadrunners-paradise.de/Images/Programm/651910588_2774061836264243_4989135014726224547_n.jpg"
        event.sourceUrl shouldBe baseUrl
        event.sourceId shouldBe "roadrunner:2026-05-29-berlin-freak-burlesque-circus"
        event.description.shouldNotBeNull()
        event.description shouldContain "LINE-UP"
        // Header intro and the flyer/ticket lines must not leak into the description.
        event.description shouldContain "Supported by MARIA"
    }

    @Test
    fun `splits multiple dot-separated blocks and infers each year from its weekday`() {
        val html =
            """
            <html><body>
              <p class="Stil62">Kartenreservierungen — header, no date, must be ignored.</p>
              <p class="Stil62">. . . . . . . . . . . . . . . . . . . .</p>
              <p class="Stil62">Samstag, 4. Juli:</p>
              <p class="Stil62"><span class="Stil11">THE ROCKABILLY KINGS</span></p>
              <p class="Stil62">Einlass: 21:00 Uhr</p>
              <p class="Stil65">A rowdy night of roots rock.</p>
              <p class="Stil62">. . . . . . . . . . . . . . . . . . . .</p>
              <p class="Stil62">Freitag, 14. August:</p>
              <p class="Stil62"><span class="Stil11">BLUES EXPLOSION</span></p>
              <p class="Stil62">Einlass: 20:00 Uhr</p>
              <p class="Stil62">. . . . . . . . . . . . . . . . . . . .</p>
              <p class="Stil62">Roadrunners Rock Motor Club — footer, no date.</p>
            </body></html>
            """.trimIndent()

        val events = scraper.scrape(Jsoup.parse(html, baseUrl), baseUrl)
        events shouldHaveSize 2

        events[0].title shouldBe "THE ROCKABILLY KINGS"
        events[0].eventDate shouldBe LocalDate.of(2026, 7, 4) // Saturday
        events[0].doorsTime shouldBe LocalTime.of(21, 0)
        events[0].description shouldBe "A rowdy night of roots rock."

        events[1].title shouldBe "BLUES EXPLOSION"
        events[1].eventDate shouldBe LocalDate.of(2026, 8, 14) // Friday
        events[1].description shouldBe null
    }

    @Test
    fun `returns no events when the page has no dated blocks`() {
        val html =
            """
            <html><body>
              <p>Welcome to Roadrunner's Paradise</p>
              <p>. . . . . . . . . .</p>
              <p>Kartenreservierungen: info@roadrunners-paradise.de</p>
            </body></html>
            """.trimIndent()
        scraper.scrape(Jsoup.parse(html, baseUrl), baseUrl) shouldHaveSize 0
    }
}
