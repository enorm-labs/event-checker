package de.norm.events.event

import de.norm.events.BaseControllerTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

class EventControllerTest : BaseControllerTest() {
    @Test
    fun `GET events returns only upcoming events with pagination metadata`(): Unit =
        runBlocking {
            val venueId = insertVenue("Astra", "astra")
            insertEvent(venueId, "Today Show", "today-show", LocalDate.now())
            insertEvent(venueId, "Future Show", "future-show", LocalDate.now().plusDays(5))
            insertEvent(venueId, "Past Show", "past-show", LocalDate.now().minusDays(5))

            webTestClient
                .get()
                .uri("/events?size=1")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(2)
                .jsonPath("$.totalPages")
                .isEqualTo(2)
                .jsonPath("$.page")
                .isEqualTo(0)
                .jsonPath("$.size")
                .isEqualTo(1)
                .jsonPath("$.content.length()")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("today-show")
                .jsonPath("$.content[0].venue.slug")
                .isEqualTo("astra")
        }

    @Test
    fun `GET events filters by venue slug`(): Unit =
        runBlocking {
            val astra = insertVenue("Astra", "astra")
            val lido = insertVenue("Lido", "lido")
            insertEvent(astra, "At Astra", "at-astra", LocalDate.now())
            insertEvent(lido, "At Lido", "at-lido", LocalDate.now())

            webTestClient
                .get()
                .uri("/events?venue=lido")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("at-lido")
        }

    @Test
    fun `GET events filters by genre slug and artist slug`(): Unit =
        runBlocking {
            val venueId = insertVenue("Astra", "astra")
            val punk = insertGenreTag("Punk", "punk")
            val techno = insertGenreTag("Techno", "techno")
            val artist = insertArtist("The Adicts", "the-adicts")

            val punkEvent = insertEvent(venueId, "Punk Night", "punk-night", LocalDate.now())
            linkGenre(punkEvent, punk)
            linkArtist(punkEvent, artist)

            val technoEvent = insertEvent(venueId, "Techno Night", "techno-night", LocalDate.now())
            linkGenre(technoEvent, techno)

            webTestClient
                .get()
                .uri("/events?genre=punk")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("punk-night")
                .jsonPath("$.content[0].genreTags[0]")
                .isEqualTo("Punk")
                .jsonPath("$.content[0].artistNames[0]")
                .isEqualTo("The Adicts")

            webTestClient
                .get()
                .uri("/events?artist=the-adicts")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("punk-night")
        }

    @Test
    fun `GET events filters by price range and search query`(): Unit =
        runBlocking {
            val venueId = insertVenue("Astra", "astra")
            insertEvent(venueId, "Cheap Gig", "cheap-gig", LocalDate.now(), pricePresale = BigDecimal("10.00"))
            insertEvent(venueId, "Pricey Gig", "pricey-gig", LocalDate.now(), pricePresale = BigDecimal("80.00"))

            webTestClient
                .get()
                .uri("/events?minPrice=50")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("pricey-gig")

            webTestClient
                .get()
                .uri("/events?q=cheap")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(1)
                .jsonPath("$.content[0].slug")
                .isEqualTo("cheap-gig")
        }

    @Test
    fun `GET events price filter falls back to box-office price when presale is unknown`(): Unit =
        runBlocking {
            val venueId = insertVenue("Astra", "astra")
            // Presale known — matched on presale.
            insertEvent(venueId, "Presale Gig", "presale-gig", LocalDate.now(), pricePresale = BigDecimal("80.00"))
            // No presale, but a box-office price within range — matched via COALESCE fallback.
            insertEvent(venueId, "Door Gig", "door-gig", LocalDate.now(), priceBoxOffice = BigDecimal("60.00"))
            // Box-office price below the bound — excluded.
            insertEvent(venueId, "Door Cheap", "door-cheap", LocalDate.now(), priceBoxOffice = BigDecimal("20.00"))
            // No price at all — excluded by any bound.
            insertEvent(venueId, "Free Gig", "free-gig", LocalDate.now())

            webTestClient
                .get()
                .uri("/events?minPrice=50")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(2)
                .jsonPath("$.content[?(@.slug == 'door-gig')]")
                .exists()
                .jsonPath("$.content[?(@.slug == 'presale-gig')]")
                .exists()
                .jsonPath("$.content[?(@.slug == 'free-gig')]")
                .doesNotExist()
                .jsonPath("$.content[?(@.slug == 'door-cheap')]")
                .doesNotExist()
        }

    @Test
    fun `GET events orders same-day events by start time`(): Unit =
        runBlocking {
            val venueId = insertVenue("Astra", "astra")
            val date = LocalDate.now().plusDays(1)
            insertEvent(venueId, "Late Show", "late-show", date, startTime = LocalTime.of(22, 0))
            insertEvent(venueId, "Early Show", "early-show", date, startTime = LocalTime.of(18, 0))

            webTestClient
                .get()
                .uri("/events")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.content[0].slug")
                .isEqualTo("early-show")
                .jsonPath("$.content[1].slug")
                .isEqualTo("late-show")
        }

    @Test
    fun `GET events today returns only today's events`(): Unit =
        runBlocking {
            val venueId = insertVenue("Astra", "astra")
            insertEvent(venueId, "Today", "today", LocalDate.now())
            insertEvent(venueId, "Tomorrow", "tomorrow", LocalDate.now().plusDays(1))

            webTestClient
                .get()
                .uri("/events/today")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(1)
                .jsonPath("$[0].slug")
                .isEqualTo("today")
        }

    @Test
    fun `GET events calendar returns events within range and rejects inverted range`(): Unit =
        runBlocking {
            val venueId = insertVenue("Astra", "astra")
            insertEvent(venueId, "In Range", "in-range", LocalDate.now().plusDays(3))
            insertEvent(venueId, "Out Of Range", "out-of-range", LocalDate.now().plusDays(40))

            val from = LocalDate.now()
            val to = LocalDate.now().plusDays(7)
            webTestClient
                .get()
                .uri("/events/calendar?from=$from&to=$to")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(1)
                .jsonPath("$[0].slug")
                .isEqualTo("in-range")

            webTestClient
                .get()
                .uri("/events/calendar?from=$to&to=$from")
                .exchange()
                .expectStatus()
                .isBadRequest
        }

    @Test
    fun `GET event by slug returns full detail with associations`(): Unit =
        runBlocking {
            val venueId = insertVenue("Astra", "astra", address = "Revaler Str. 99")
            val headliner = insertArtist("The Adicts", "the-adicts")
            val support = insertArtist("Maid Of Ace", "maid-of-ace")
            val promoter = insertPromoter("36 Concerts", "36-concerts")
            val punk = insertGenreTag("Punk", "punk")

            val eventId = insertEvent(venueId, "The Adicts", "the-adicts-live", LocalDate.now(), subtitle = "Tour 2026")
            linkArtist(eventId, headliner, role = "HEADLINER", billingOrder = 0)
            linkArtist(eventId, support, role = "SUPPORT", billingOrder = 1)
            linkPromoter(eventId, promoter)
            linkGenre(eventId, punk)

            webTestClient
                .get()
                .uri("/events/the-adicts-live")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.slug")
                .isEqualTo("the-adicts-live")
                .jsonPath("$.subtitle")
                .isEqualTo("Tour 2026")
                .jsonPath("$.venue.slug")
                .isEqualTo("astra")
                .jsonPath("$.lineup.length()")
                .isEqualTo(2)
                .jsonPath("$.lineup[0].artist.slug")
                .isEqualTo("the-adicts")
                .jsonPath("$.lineup[0].role")
                .isEqualTo("HEADLINER")
                .jsonPath("$.lineup[1].artist.slug")
                .isEqualTo("maid-of-ace")
                .jsonPath("$.promoters[0].slug")
                .isEqualTo("36-concerts")
                .jsonPath("$.genreTags[0]")
                .isEqualTo("Punk")
        }

    @Test
    fun `GET event by unknown slug returns 404`() {
        webTestClient
            .get()
            .uri("/events/does-not-exist")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `GET events ignores an unknown or malicious sort parameter`(): Unit =
        runBlocking {
            val venueId = insertVenue("Astra", "astra")
            insertEvent(venueId, "Later", "later", LocalDate.now().plusDays(2))
            insertEvent(venueId, "Sooner", "sooner", LocalDate.now().plusDays(1))

            // An unmapped sort property (here a SQL-injection attempt) is dropped by the
            // ORDER BY whitelist, so the request succeeds with the default chronological order
            // rather than erroring or executing the injected text.
            webTestClient
                .get()
                .uri("/events?sort={sort}", "event_date; DROP TABLE events.event;--")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(2)
                .jsonPath("$.content[0].slug")
                .isEqualTo("sooner")
                .jsonPath("$.content[1].slug")
                .isEqualTo("later")

            // The table is intact — the injection string was bound/ignored as data, not run as SQL.
            webTestClient
                .get()
                .uri("/events")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.totalElements")
                .isEqualTo(2)
        }
}
