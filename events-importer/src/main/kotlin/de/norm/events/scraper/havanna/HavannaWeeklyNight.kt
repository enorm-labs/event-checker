package de.norm.events.scraper.havanna

import de.norm.events.event.EventType
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.ScrapedEvent
import java.math.BigDecimal
import java.net.URI
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * A single **undated, weekly recurring** Havanna club night, as described by one of the venue's
 * `/wednesday`, `/friday`, `/saturday` pages.
 *
 * Havanna is the one scraped venue that publishes no dated programme at all: it runs the same three
 * resident nights every week and its pages carry only a weekday, never a calendar date. This class is
 * therefore the intermediate representation the detail scraper produces — everything a night needs
 * *except* a date — and [toScrapedEvents] turns it into the concrete dated [ScrapedEvent]s the rest of
 * the pipeline expects, one per week over a rolling horizon.
 *
 * @see HavannaDetailPageScraper for the parsing that produces this.
 */
data class HavannaWeeklyNight(
    /** Weekday this night runs on, derived from the page's URL path (`/friday` → [DayOfWeek.FRIDAY]). */
    val dayOfWeek: DayOfWeek,
    /** Last path segment of the night's page URL (`friday`) — the stable identity used in `sourceId`. */
    val slug: String,
    /** The night's name, e.g. "Saturdays @ HAVANNA". */
    val title: String,
    /** The night's tagline, e.g. "Party auf 3 Dancefloors!". */
    val subtitle: String? = null,
    /** Floor-by-floor programme, times, and pricing prose, one source paragraph per line. */
    val description: String? = null,
    /** Raw floor genres joined for the event's display genre, e.g. "Reggaeton, Latin-Pop, Hip Hop, RnB & Oldschool". */
    val genre: String? = null,
    /** When the party starts, from an explicit "Start:"/"Party:" line or the footer's opening hours. */
    val startTime: LocalTime? = null,
    /** Door price ("Entrance Fee: 14,00 €"); Havanna sells no presale tickets. */
    val priceBoxOffice: BigDecimal? = null,
    /** Poster for the night, from the detail page or the overview teaser. */
    val imageUrl: String? = null,
    /** The night page's URL — every generated occurrence points back at it. */
    val sourceUrl: String,
    /**
     * First day of an announced closure ("WIR SIND AB DEM 01.07.2026 IN DER SOMMERPAUSE!"), or `null`
     * when the page announces none. Occurrences on or after this date are not generated.
     */
    val pauseFrom: LocalDate? = null
) {
    /**
     * Expands this weekly night into one dated [ScrapedEvent] per week, starting with the next
     * occurrence of [dayOfWeek] on or after today and running for [weeks] weeks.
     *
     * The horizon exists because the venue asserts a standing weekly programme rather than publishing
     * dates: every import regenerates the same rolling window, and the stable `sourceId`
     * (`havanna:<date>-<slug>`) makes that idempotent — occurrences that roll out of the window are
     * cleaned up as stale by `EventUpsertService`, which also drops anything past-dated.
     *
     * Occurrences on or after [pauseFrom] are omitted: during an announced break the night does not
     * happen, and the venue gives no end date to resume from.
     *
     * @param clock clock supplying "today"; override in tests for determinism.
     * @param weeks how many weekly occurrences to generate.
     */
    fun toScrapedEvents(
        clock: Clock,
        weeks: Int = OCCURRENCE_WEEKS
    ): List<ScrapedEvent> {
        val firstOccurrence = LocalDate.now(clock).with(TemporalAdjusters.nextOrSame(dayOfWeek))
        return (0 until weeks)
            .map { firstOccurrence.plusWeeks(it.toLong()) }
            .filter { pauseFrom == null || it < pauseFrom }
            .map { date -> toScrapedEvent(date) }
    }

    private fun toScrapedEvent(date: LocalDate): ScrapedEvent =
        ScrapedEvent(
            title = title,
            subtitle = subtitle,
            description = description,
            // Every Havanna night is a resident-DJ dance party.
            eventType = EventType.PARTY.name,
            eventDate = date,
            startTime = startTime,
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            sourceId = "${EventSource.HAVANNA.sourceIdPrefix}$date-$slug",
            genre = genre,
            // The door price is the only price the venue quotes — there is no presale.
            priceBoxOffice = priceBoxOffice
            // priceNote is deliberately left null: the venue's only pricing aside is the
            // "(Ladies von 22:00 – 23:00 for free!)" line, and `detectFree` scans priceNote for a
            // standalone "free" token — putting it there would flag the whole night as free entry.
            // The line is preserved in `description` instead.
        )

    companion object {
        /**
         * How many weekly occurrences each import generates per night (~2 months of calendar).
         *
         * Deep enough that Havanna shows up in a month-ahead view, shallow enough that the programme
         * stays a plausible assertion — the venue never publishes dates, so every generated occurrence
         * is derived from its standing weekly schedule rather than an announcement.
         */
        const val OCCURRENCE_WEEKS: Int = 8
    }
}

/** English weekday page paths Havanna uses for its resident nights, mapped to the weekday they run on. */
private val WEEKDAY_PATHS: Map<String, DayOfWeek> =
    mapOf(
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY
    )

/**
 * The last path segment of a Havanna night URL — `https://www.havanna-berlin.de/friday` → `friday`.
 *
 * This is the night's stable identity: the page path never changes, whereas the headline on it does.
 */
internal fun havannaNightSlug(url: String): String =
    URI(url)
        .path
        .trim('/')
        .substringAfterLast('/')
        .lowercase()

/**
 * The weekday a Havanna night page describes, read from its URL path, or `null` when the path names no
 * weekday (e.g. the `/events` overview or the "‹ Back to Events" link).
 *
 * The path is the only machine-readable weekday on these pages — the heading that repeats it
 * ("Friday") is editorial copy — and it doubles as the filter that tells night links apart from the
 * site's other buttons.
 */
internal fun havannaWeekdayFromUrl(url: String): DayOfWeek? = WEEKDAY_PATHS[havannaNightSlug(url)]

/**
 * The weekday an English day name refers to ("Saturday" → [DayOfWeek.SATURDAY]), or `null` when it
 * names none. Used to read the footer's opening-hours lines, which are keyed by the same day names as
 * the night page paths.
 */
internal fun havannaWeekdayFromName(name: String): DayOfWeek? = WEEKDAY_PATHS[name.trim().lowercase()]
