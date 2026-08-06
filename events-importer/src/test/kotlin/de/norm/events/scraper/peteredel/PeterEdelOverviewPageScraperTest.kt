package de.norm.events.scraper.peteredel

import de.norm.events.scraper.ScrapedArtist
import de.norm.events.scraper.ScrapedEvent
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [PeterEdelOverviewPageScraper].
 *
 * Driven by a real snapshot of `/events/`, which covers the shapes that matter: seven month headings
 * supplying the year for their year-less rows (including two that roll into the next year), separate
 * and collapsed doors/start labels, a transposed pair, from-prices and TBA box-office lines, a
 * sold-out event, two cancelled ones, support billings, and a middle column whose `Tickets:` line
 * holds a sale-start date rather than a price. Hand-built documents cover what the live page does not
 * currently show.
 */
class PeterEdelOverviewPageScraperTest {
    private val scraper = PeterEdelOverviewPageScraper()
    private val baseUrl = "https://www.peteredel.de/events/"
    private lateinit var events: List<ScrapedEvent>

    @BeforeEach
    fun setUp() {
        val html =
            javaClass.classLoader
                .getResourceAsStream("scraper/peteredel/peteredel-overview.html")!!
                .bufferedReader()
                .readText()
        events = scraper.scrape(Jsoup.parse(html, baseUrl), baseUrl)
    }

    /** Builds a grid holding one month heading and one event box with the given main-column markup. */
    private fun grid(
        heading: String?,
        dateLine: String,
        main: String,
        ticketColumn: String = "<h3>Tickets: 10,00€</h3>"
    ) = Jsoup.parse(
        """<div class="grid-section">
           |${heading?.let { """<div><h1>$it</h1></div>""" }.orEmpty()}
           |<div class="box-rc-dark-grey"><div class="container"><div class="row clearfix">
           |<div class="col-md-2 column"><div><div><h3>$dateLine</h3></div><p><img src="/media/1/f.jpg"></p></div></div>
           |<div class="col-md-8 column"><div>$main</div></div>
           |<div class="col-md-2 column"><div>$ticketColumn</div></div>
           |</div></div></div></div>
        """.trimMargin(),
        baseUrl
    )

    @Test
    fun `extracts every event in listing order`() {
        events shouldHaveSize 39
        events.first().eventDate shouldBe LocalDate.of(2026, 8, 20)
        events.last().eventDate shouldBe LocalDate.of(2027, 5, 22)
    }

    @Test
    fun `parses every field of a concert`() {
        val polica = events.single { it.title.startsWith("Poliça") }
        polica.title shouldBe "Poliça - Dreams Go Tour 2026"
        polica.subtitle shouldBe "Support: Sian Able"
        polica.eventDate shouldBe LocalDate.of(2026, 8, 25)
        polica.doorsTime shouldBe LocalTime.of(19, 0)
        polica.startTime shouldBe LocalTime.of(20, 0)
        polica.sourceUrl shouldBe baseUrl
        polica.sourceId shouldBe "peter_edel:2026-08-25-polica-dreams-go-tour-2026"
        polica.ticketUrl shouldBe "https://tickets.loft.de/event/polica-92q9t2"
        polica.imageUrl shouldBe "https://www.peteredel.de/media/2598/polica.jpg"
        polica.pricePresale shouldBe BigDecimal("25.00")
        polica.priceBoxOffice shouldBe BigDecimal("32")
        polica.priceNote shouldBe "Tickets: 25,00€ (zzgl. VVK-Gebühr) Abendkasse: 32 Euro"
        polica.status shouldBe "SCHEDULED"
        polica.soldOut shouldBe false
        polica.free shouldBe false
        polica.promoters shouldContainExactly listOf("Loft Concerts")
        polica.description!! shouldContain "die beste Band aller Zeiten"
        // The venue states no category and its programme is mixed, so nothing types this as a concert.
        polica.eventType shouldBe "OTHER"
        polica.artists shouldContainExactly
            listOf(
                ScrapedArtist("Poliça", "HEADLINER"),
                ScrapedArtist("Sian Able", "SUPPORT")
            )
    }

    @Test
    fun `takes the year from the month heading above each row`() {
        // The rows carry no year at all; only the headings do, and the programme crosses into 2027.
        events.single { it.title == "ERIKA VIKMAN" }.eventDate shouldBe LocalDate.of(2027, 3, 8)
        events.single { it.title == "MAMA LEISA" }.eventDate shouldBe LocalDate.of(2027, 5, 22)
        events.filter { it.title == "Tanztee im PETER EDEL" }.map { it.eventDate } shouldContainExactly
            listOf(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1))
    }

    @Test
    fun `reads a collapsed doors and start label as one time`() {
        val afterWork = events.first { it.title.startsWith("AFTER WORK") }
        // "Beginn & Einlass: 18:00 Uhr" — one time serving as both.
        afterWork.doorsTime shouldBe LocalTime.of(18, 0)
        afterWork.startTime shouldBe LocalTime.of(18, 0)
        afterWork.eventType shouldBe "PARTY"
    }

    @Test
    fun `keeps a transposed doors and start pair as written for the persistence boundary to fix`() {
        // The venue wrote "Einlass: 15:00 Uhr Beginn: 14:00 Uhr"; orderDoorsBeforeStart swaps it on save.
        val carmenMaja = events.single { it.title.startsWith("Carmen Maja") }
        carmenMaja.doorsTime shouldBe LocalTime.of(15, 0)
        carmenMaja.startTime shouldBe LocalTime.of(14, 0)
    }

    @Test
    fun `marks a sold-out event and stores no price for it`() {
        val carmenMaja = events.single { it.title.startsWith("Carmen Maja") }
        carmenMaja.soldOut shouldBe true
        carmenMaja.pricePresale.shouldBeNull()
        carmenMaja.priceBoxOffice.shouldBeNull()
        carmenMaja.promoters shouldContainExactly listOf("Volkssolidarität Berlin")
    }

    @Test
    fun `strips the cancelled marker from the title and sets the status`() {
        val cancelled = events.filter { it.status == "CANCELLED" }
        cancelled shouldHaveSize 2
        cancelled.map { it.title } shouldContainExactly
            listOf("Kindertheater: Däumelinchen", "Kindertheater: Die Schneekönigin")
        // The marker is out of the identity too, so reinstating the show keeps the same row.
        cancelled.first().sourceId shouldBe "peter_edel:2026-10-04-kindertheater-daumelinchen"
        // "Leider abgesagt!" is a status, not a pricing note.
        cancelled.first().priceNote.shouldBeNull()
        cancelled.first().eventType shouldBe "SHOW"
    }

    @Test
    fun `reads prices from the ticket column rather than the prose`() {
        // The middle column reads "Tickets: Ab 17.04.26 - online … erhältlich!" — a sale-start date
        // that would parse as a number; the ticket column carries the real price.
        val emmaHarner = events.single { it.title == "Emma Harner" }
        emmaHarner.pricePresale shouldBe BigDecimal("25.00")
        emmaHarner.priceBoxOffice shouldBe BigDecimal("30")
        emmaHarner.subtitle shouldBe "\"The Evening Star\"-Tour"
    }

    @Test
    fun `keeps a from-price qualifier and a tier list in the price note`() {
        val nosferatu = events.single { it.title.startsWith("NOSFERATU") }
        nosferatu.pricePresale shouldBe BigDecimal("25.00")
        nosferatu.priceBoxOffice shouldBe BigDecimal("28")
        nosferatu.priceNote shouldBe "Tickets: Ab 25,00€ (inkl. VVK-Gebühr) Abendkasse Ab 28 Euro"
        nosferatu.eventType shouldBe "SCREENING"

        val talkshow = events.single { it.title.startsWith("Talkshow") }
        talkshow.priceBoxOffice shouldBe BigDecimal("30")
        talkshow.priceNote shouldBe "Tickets: Ab 27,00€ (inkl. VVK-Gebühr) Abendkasse 30 Euro / 35 Euro"
        talkshow.eventType shouldBe "SHOW"
    }

    @Test
    fun `stores no box-office price for a TBA line`() {
        val timonKrause = events.single { it.title == "Timon Krause" }
        timonKrause.pricePresale shouldBe BigDecimal("29.90")
        timonKrause.priceBoxOffice.shouldBeNull()
        timonKrause.priceNote shouldBe "Tickets: 29,90€ (inkl. VVK-Gebühr) Abendkasse TBA"
    }

    @Test
    fun `reads the Tageskasse line as the box-office price`() {
        val tanztee = events.first { it.title == "Tanztee im PETER EDEL" }
        tanztee.pricePresale shouldBe BigDecimal("11.34")
        tanztee.priceBoxOffice shouldBe BigDecimal("11.34")
        tanztee.eventType shouldBe "PARTY"
    }

    @Test
    fun `takes artists only where the venue bills a support act`() {
        val withArtists = events.filter { it.artists.isNotEmpty() }
        withArtists.map { it.title } shouldContainExactly listOf("Poliça - Dreams Go Tour 2026", "Buffalo Tom")
        withArtists[1].artists shouldContainExactly
            listOf(
                ScrapedArtist("Buffalo Tom", "HEADLINER"),
                ScrapedArtist("Grateful Cat", "SUPPORT")
            )
    }

    @Test
    fun `recovers a reading from the subtitle`() {
        events.single { it.title.startsWith("Mikis Wesensbitter") }.eventType shouldBe "READING"
    }

    @Test
    fun `never flags a paid event as free`() {
        events.none { it.free } shouldBe true
    }

    @Test
    fun `skips a box that has no month heading above it`() {
        scraper.scrape(grid(heading = null, dateLine = "DO | 20.08.", main = "<h3>Something</h3>"), baseUrl).shouldBeEmpty()
    }

    @Test
    fun `skips a box with an unparseable date line`() {
        scraper.scrape(grid("AUGUST 2026", "demnächst", "<h3>Something</h3>"), baseUrl).shouldBeEmpty()
    }

    @Test
    fun `skips a box with an impossible date`() {
        scraper.scrape(grid("AUGUST 2026", "DO | 31.02.", "<h3>Something</h3>"), baseUrl).shouldBeEmpty()
    }

    @Test
    fun `skips a box with no title`() {
        scraper.scrape(grid("AUGUST 2026", "DO | 20.08.", "<p>Einlass: 19:00 Uhr</p>"), baseUrl).shouldBeEmpty()
    }

    @Test
    fun `accepts an event announced without times, prices or a subtitle`() {
        val parsed = scraper.scrape(grid("AUGUST 2026", "DO | 20.08.", "<h3>Something</h3>", ticketColumn = ""), baseUrl)
        parsed shouldHaveSize 1
        parsed[0].title shouldBe "Something"
        parsed[0].subtitle.shouldBeNull()
        parsed[0].doorsTime.shouldBeNull()
        parsed[0].startTime.shouldBeNull()
        parsed[0].pricePresale.shouldBeNull()
        parsed[0].priceNote.shouldBeNull()
        parsed[0].promoters.shouldBeEmpty()
        parsed[0].artists.shouldBeEmpty()
        parsed[0].eventType shouldBe "OTHER"
    }

    @Test
    fun `returns no events for a page without a programme grid`() {
        scraper.scrape(Jsoup.parse("<html><body><main></main></body></html>", baseUrl), baseUrl).shouldBeEmpty()
    }
}
