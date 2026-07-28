package de.norm.events.scraper.havanna

import de.norm.events.event.EventType
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Unit tests for [HavannaWeeklyNight.toScrapedEvents] — the recurrence expansion that turns Havanna's
 * undated weekly nights into the dated events the rest of the pipeline expects.
 *
 * The clock is pinned to Tuesday 2026-07-28 so the generated occurrence dates are deterministic.
 */
class HavannaWeeklyNightTest {
    private val clock: Clock = Clock.fixed(Instant.parse("2026-07-28T10:00:00Z"), ZoneOffset.UTC)

    private fun night(
        dayOfWeek: DayOfWeek = DayOfWeek.FRIDAY,
        slug: String = "friday",
        pauseFrom: LocalDate? = null
    ) = HavannaWeeklyNight(
        dayOfWeek = dayOfWeek,
        slug = slug,
        title = "Die große Party von Salsa bis Black Music!",
        subtitle = "Ladies Night & Reggeaton & noch viel mehr...",
        description = "1st floor Latin Dance Party",
        genre = "Reggaeton, Latin-Pop",
        startTime = LocalTime.of(22, 0),
        priceBoxOffice = BigDecimal("10.00"),
        imageUrl = "https://images.squarespace-cdn.com/friday.jpeg",
        sourceUrl = "https://www.havanna-berlin.de/friday",
        pauseFrom = pauseFrom
    )

    @Test
    fun `generates one occurrence per week over the rolling horizon`() {
        val events = night().toScrapedEvents(clock)

        events shouldHaveSize HavannaWeeklyNight.OCCURRENCE_WEEKS
        events.map { it.eventDate } shouldContainExactly
            listOf(
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 14),
                LocalDate.of(2026, 8, 21),
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 18)
            )
    }

    @Test
    fun `includes today when today is already the night's weekday`() {
        // Wednesday 2026-07-29 is the day after the pinned clock; a Tuesday night would start today.
        night(dayOfWeek = DayOfWeek.TUESDAY, slug = "tuesday")
            .toScrapedEvents(clock)
            .first()
            .eventDate shouldBe LocalDate.of(2026, 7, 28)
    }

    @Test
    fun `carries the night's fields onto every occurrence with a stable dated sourceId`() {
        val event = night().toScrapedEvents(clock).first()

        event.title shouldBe "Die große Party von Salsa bis Black Music!"
        event.subtitle shouldBe "Ladies Night & Reggeaton & noch viel mehr..."
        // Every Havanna night is a resident-DJ dance party.
        event.eventType shouldBe EventType.PARTY.name
        event.startTime shouldBe LocalTime.of(22, 0)
        event.priceBoxOffice shouldBe BigDecimal("10.00")
        event.genre shouldBe "Reggaeton, Latin-Pop"
        event.imageUrl shouldBe "https://images.squarespace-cdn.com/friday.jpeg"
        // Occurrences point back at the night page — there are no per-date URLs.
        event.sourceUrl shouldBe "https://www.havanna-berlin.de/friday"
        event.sourceId shouldBe "havanna:2026-07-31-friday"
        // Leaving the price note empty keeps the "for free" in the ladies' window from flagging the
        // whole night as free entry.
        event.priceNote shouldBe null
        event.free shouldBe false
    }

    @Test
    fun `gives every occurrence its own sourceId`() {
        val ids = night().toScrapedEvents(clock).map { it.sourceId }
        ids.distinct() shouldHaveSize HavannaWeeklyNight.OCCURRENCE_WEEKS
        ids.last() shouldBe "havanna:2026-09-18-friday"
    }

    @Test
    fun `suppresses occurrences on or after an announced closure`() {
        // Announced mid-horizon: the first two Fridays still run, the rest fall in the break.
        val events = night(pauseFrom = LocalDate.of(2026, 8, 14)).toScrapedEvents(clock)

        events.map { it.eventDate } shouldContainExactly
            listOf(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 8, 7))
    }

    @Test
    fun `generates nothing while a closure announced in the past is still in force`() {
        // The venue's live state: "IN DER SOMMERPAUSE" since 01.07.2026, with no end date given.
        night(dayOfWeek = DayOfWeek.WEDNESDAY, slug = "wednesday", pauseFrom = LocalDate.of(2026, 7, 1))
            .toScrapedEvents(clock)
            .shouldBeEmpty()
    }
}
