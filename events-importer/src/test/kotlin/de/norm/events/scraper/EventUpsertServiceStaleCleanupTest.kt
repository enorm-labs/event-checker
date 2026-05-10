package de.norm.events.scraper

import de.norm.events.artist.ArtistRepository
import de.norm.events.event.EventArtistRepository
import de.norm.events.event.EventEntity
import de.norm.events.event.EventRepository
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Unit tests for stale event cleanup logic in [EventUpsertService].
 *
 * Uses a fixed clock pinned to 2026-06-15 so all "today"/"tomorrow" calculations
 * are deterministic. Tests exercise `removeStaleEvents` indirectly through the
 * public [EventUpsertService.upsertAndCleanup] method.
 */
class EventUpsertServiceStaleCleanupTest {
    private val eventRepository: EventRepository = mockk(relaxed = true)
    private val eventArtistRepository: EventArtistRepository = mockk(relaxed = true)
    private val artistRepository: ArtistRepository = mockk(relaxed = true)

    /** Fixed "today" for all tests — 2026-06-15. */
    private val today = LocalDate.of(2026, 6, 15)
    private val tomorrow = today.plusDays(1)
    private val fixedClock: Clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var service: EventUpsertService

    private val venueId = 10L
    private val eventSourceId = 1L

    /** Creates a minimal [ScrapedEvent] for testing. */
    private fun scrapedEvent(
        title: String = "Test Event",
        eventDate: LocalDate,
        sourceId: String
    ) = ScrapedEvent(
        title = title,
        eventDate = eventDate,
        sourceId = sourceId,
        sourceUrl = "https://example.com/event/test",
        eventType = "CONCERT",
        status = "SCHEDULED"
    )

    /** Creates a minimal [EventEntity] representing a persisted event. */
    private fun existingEvent(
        id: Long,
        eventDate: LocalDate,
        sourceId: String,
        title: String = "Existing Event"
    ) = EventEntity(
        id = id,
        venueId = venueId,
        title = title,
        slug = "slug-$id",
        eventDate = eventDate,
        sourceId = sourceId,
        eventSourceId = eventSourceId
    )

    @BeforeEach
    fun setUp() {
        service =
            EventUpsertService(
                eventRepository = eventRepository,
                eventArtistRepository = eventArtistRepository,
                artistRepository = artistRepository,
                clock = fixedClock
            )

        // Default stubs for the upsert pipeline (we're testing stale cleanup, not upsert)
        coEvery { eventRepository.findBySourceIdIn(any()) } returns emptyFlow()
        coEvery { eventRepository.deleteByIdIn(any()) } returns Unit
        coEvery { eventArtistRepository.findByEventIdIn(any()) } returns emptyFlow()
        coEvery { eventRepository.saveAll(any<Iterable<EventEntity>>()) } answers {
            firstArg<Iterable<EventEntity>>()
                .mapIndexed { index, entity -> entity.copy(id = entity.id ?: (100L + index)) }
                .asFlow()
        }
    }

    @Nested
    inner class TomorrowLowerBound {
        @Test
        fun `does not delete today's event even if missing from scraped results`() =
            runTest {
                // Scenario: A today-event exists in DB but the venue website no longer
                // lists it (common when venues show only "upcoming" from tomorrow).
                // The scraper returns only a tomorrow event.
                val scrapedEvents =
                    listOf(
                        scrapedEvent(title = "Tomorrow Gig", eventDate = tomorrow, sourceId = "src:tomorrow-gig")
                    )

                val tomorrowEvent = existingEvent(id = 2L, eventDate = tomorrow, sourceId = "src:tomorrow-gig")

                // The repository query uses tomorrow..maxScrapedDate, so today's event
                // should never even be returned by the query.
                val fromDateSlot = slot<LocalDate>()
                coEvery {
                    eventRepository.findByEventSourceIdAndEventDateBetween(
                        eventSourceId = eventSourceId,
                        fromDate = capture(fromDateSlot),
                        toDate = any()
                    )
                } returns listOf(tomorrowEvent).asFlow()

                service.upsertAndCleanup(scrapedEvents, venueId, eventSourceId)

                // Verify the query lower bound is tomorrow, not today
                fromDateSlot.captured shouldBe tomorrow

                // Today's event should NOT be deleted
                coVerify(exactly = 0) {
                    eventRepository.deleteByIdIn(match { 1L in it })
                }
            }

        @Test
        fun `deletes stale tomorrow event that is no longer listed`() =
            runTest {
                // Scenario: Tomorrow's event was previously imported but is now gone
                // from the website (genuinely cancelled). Another event exists for the
                // day after tomorrow.
                val dayAfterTomorrow = tomorrow.plusDays(1)

                val scrapedEvents =
                    listOf(
                        scrapedEvent(
                            title = "Day After Tomorrow Gig",
                            eventDate = dayAfterTomorrow,
                            sourceId = "src:day-after"
                        )
                    )

                val staleEvent = existingEvent(id = 1L, eventDate = tomorrow, sourceId = "src:cancelled-gig")
                val activeEvent = existingEvent(id = 2L, eventDate = dayAfterTomorrow, sourceId = "src:day-after")

                coEvery {
                    eventRepository.findByEventSourceIdAndEventDateBetween(any(), any(), any())
                } returns listOf(staleEvent, activeEvent).asFlow()

                service.upsertAndCleanup(scrapedEvents, venueId, eventSourceId)

                // Tomorrow's stale event should be deleted
                coVerify {
                    eventRepository.deleteByIdIn(match { 1L in it && 2L !in it })
                }
            }

        @Test
        fun `does not delete today's cancelled event — it expires naturally`() =
            runTest {
                // Scenario: Today's event was genuinely cancelled (removed from website).
                // The scraper returns events starting from 3 days out.
                // The event stays in DB until it becomes a past event — acceptable trade-off.
                val threeDaysOut = today.plusDays(3)
                val fourDaysOut = today.plusDays(4)

                val scrapedEvents =
                    listOf(
                        scrapedEvent(title = "Future Gig 1", eventDate = threeDaysOut, sourceId = "src:future-1"),
                        scrapedEvent(title = "Future Gig 2", eventDate = fourDaysOut, sourceId = "src:future-2")
                    )

                // Query starts from tomorrow, so today's event won't be in the result set
                coEvery {
                    eventRepository.findByEventSourceIdAndEventDateBetween(any(), any(), any())
                } returns emptyFlow()

                service.upsertAndCleanup(scrapedEvents, venueId, eventSourceId)

                // No deletions — today's cancelled event is outside the cleanup window
                coVerify(exactly = 0) {
                    eventRepository.deleteByIdIn(any())
                }
            }
    }

    @Nested
    inner class DateRangeBounds {
        @Test
        fun `cleanup window is tomorrow to max scraped date`() =
            runTest {
                // Verify that findByEventSourceIdAndEventDateBetween is called
                // with exactly tomorrow as fromDate and the latest scraped date as toDate.
                val latestDate = today.plusDays(30)

                val scrapedEvents =
                    listOf(
                        scrapedEvent(title = "Near Event", eventDate = tomorrow, sourceId = "src:near"),
                        scrapedEvent(title = "Far Event", eventDate = latestDate, sourceId = "src:far")
                    )

                val fromDateSlot = slot<LocalDate>()
                val toDateSlot = slot<LocalDate>()
                coEvery {
                    eventRepository.findByEventSourceIdAndEventDateBetween(
                        eventSourceId = eventSourceId,
                        fromDate = capture(fromDateSlot),
                        toDate = capture(toDateSlot)
                    )
                } returns emptyFlow()

                service.upsertAndCleanup(scrapedEvents, venueId, eventSourceId)

                fromDateSlot.captured shouldBe tomorrow
                toDateSlot.captured shouldBe latestDate
            }

        @Test
        fun `does not delete events beyond the max scraped date`() =
            runTest {
                // Scenario: Events exist in the DB beyond the scraper's date range
                // (e.g. from a previous deeper scrape). They should not be touched.
                val scrapedDate = tomorrow

                val scrapedEvents =
                    listOf(
                        scrapedEvent(title = "Tomorrow Gig", eventDate = scrapedDate, sourceId = "src:tomorrow")
                    )

                val tomorrowEvent = existingEvent(id = 1L, eventDate = scrapedDate, sourceId = "src:tomorrow")
                // This event is beyond maxScrapedDate — it won't be in the query results
                // because the repository query is bounded by toDate=scrapedDate

                val toDateSlot = slot<LocalDate>()
                coEvery {
                    eventRepository.findByEventSourceIdAndEventDateBetween(
                        eventSourceId = any(),
                        fromDate = any(),
                        toDate = capture(toDateSlot)
                    )
                } returns listOf(tomorrowEvent).asFlow()

                service.upsertAndCleanup(scrapedEvents, venueId, eventSourceId)

                // Upper bound should be the max scraped date (tomorrow), not far future
                toDateSlot.captured shouldBe scrapedDate

                // No deletions — the only event in range is still in the scraped list
                coVerify(exactly = 0) {
                    eventRepository.deleteByIdIn(any())
                }
            }

        @Test
        fun `all scraped events on same date uses that date as both min and max`() =
            runTest {
                // Scenario: All scraped events are for the same day (e.g. a single-day festival).
                // The cleanup window should be tomorrow..thatDate. If all events are for tomorrow,
                // both bounds collapse to the same date.
                val scrapedEvents =
                    listOf(
                        scrapedEvent(title = "Festival Act 1", eventDate = tomorrow, sourceId = "src:act-1"),
                        scrapedEvent(title = "Festival Act 2", eventDate = tomorrow, sourceId = "src:act-2")
                    )

                val staleEvent = existingEvent(id = 3L, eventDate = tomorrow, sourceId = "src:cancelled-act")
                val activeEvents =
                    listOf(
                        existingEvent(id = 1L, eventDate = tomorrow, sourceId = "src:act-1"),
                        existingEvent(id = 2L, eventDate = tomorrow, sourceId = "src:act-2")
                    )

                coEvery {
                    eventRepository.findByEventSourceIdAndEventDateBetween(any(), any(), any())
                } returns (activeEvents + staleEvent).asFlow()

                service.upsertAndCleanup(scrapedEvents, venueId, eventSourceId)

                // Only the stale event should be deleted
                coVerify {
                    eventRepository.deleteByIdIn(match { it == listOf(3L) })
                }
            }
    }

    @Nested
    inner class AllScrapedEventsToday {
        @Test
        fun `skips cleanup when all scraped events are for today`() =
            runTest {
                // Scenario: The scraper returns only today's events. The cleanup window
                // would be tomorrow..today which is an empty/invalid range, but the
                // maxScrapedDate (today) < tomorrow, so no existing events should be found.
                val scrapedEvents =
                    listOf(
                        scrapedEvent(title = "Today Show", eventDate = today, sourceId = "src:today-show")
                    )

                val fromDateSlot = slot<LocalDate>()
                val toDateSlot = slot<LocalDate>()
                coEvery {
                    eventRepository.findByEventSourceIdAndEventDateBetween(
                        eventSourceId = eventSourceId,
                        fromDate = capture(fromDateSlot),
                        toDate = capture(toDateSlot)
                    )
                } returns emptyFlow()

                service.upsertAndCleanup(scrapedEvents, venueId, eventSourceId)

                // The window is tomorrow..today — the repository should handle this
                // as an empty range and return no results
                fromDateSlot.captured shouldBe tomorrow
                toDateSlot.captured shouldBe today

                // No deletions should occur
                coVerify(exactly = 0) {
                    eventRepository.deleteByIdIn(any())
                }
            }
    }
}
