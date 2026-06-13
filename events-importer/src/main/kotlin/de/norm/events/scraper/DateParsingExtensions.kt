package de.norm.events.scraper

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Shared date and time parsing utilities for venue scrapers.
//
// Berlin venue websites use two common date/time formats:
// 1. Standalone HH:mm — rendered in HTML for doors/start times
//    (e.g. "Einlass: 19:00", "Beginn: 20:00"). Parsed by [parseTime].
// 2. ISO 8601 datetime — embedded in schema.org MusicEvent JSON-LD
//    startDate fields (e.g. "2026-05-16T20:00"). Split into date and
//    time by [parseIsoDate] and [parseIsoTime].
// 3. European short date DD/MM/YY — used by some WordPress-based venue
//    sites (e.g. "21/09/26"). Parsed by [parseShortDate].
//
// All functions follow a null-safe convention: they return null for
// unparseable, blank, or missing input rather than throwing exceptions.

/**
 * Sentinel for a [ScrapedEvent.eventDate] that could not be resolved on the
 * page being parsed. Two-page importers use it on the overview/detail step that
 * lacks a date (e.g. Astra's dateless featured teaser, or a Madame Claude detail
 * page with no parseable date) and rely on the other page to supply the real
 * value during merge. [AbstractTwoPageWebsiteImporter] drops any event still
 * carrying this sentinel after the merge so it never reaches persistence.
 */
val UNRESOLVED_EVENT_DATE: LocalDate = LocalDate.MIN

/** Standard 24-hour time format (HH:mm) used by most Berlin venue websites. */
private val HH_MM_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** European short date format (d/M/yy); 2-digit year resolves to 2000–2099. */
private val SHORT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yy")

/**
 * Attempts to parse [text] as a [LocalTime] using the given [formatter].
 *
 * Returns `null` if [text] is null, blank, or cannot be parsed — rather
 * than throwing an exception. This is the expected behavior for scrapers
 * where missing or malformed time values should degrade gracefully.
 *
 * Example:
 * ```kotlin
 * val doorsTime = parseTime("19:00", HH_MM_FORMATTER)  // LocalTime.of(19, 0)
 * val invalid = parseTime("TBA", HH_MM_FORMATTER)      // null
 * ```
 */
fun parseTime(
    text: String?,
    formatter: DateTimeFormatter = HH_MM_FORMATTER
): LocalTime? {
    if (text.isNullOrBlank()) return null
    return try {
        LocalTime.parse(text.trim(), formatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

/**
 * Parses the date portion from an ISO 8601 date-time string.
 *
 * Handles both full datetime (`"2026-05-16T20:00"`) and date-only
 * (`"2026-05-16"`) inputs — [String.substringBefore] returns the whole
 * string when "T" is absent.
 *
 * This is the standard date format used by schema.org `MusicEvent`
 * JSON-LD blocks (`startDate` field), which many venue websites embed
 * for SEO. Returns `null` for unparseable input.
 *
 * Example:
 * ```kotlin
 * parseIsoDate("2026-05-16T20:00")  // LocalDate.of(2026, 5, 16)
 * parseIsoDate("2026-05-16")        // LocalDate.of(2026, 5, 16)
 * parseIsoDate("invalid")           // null
 * ```
 */
fun parseIsoDate(dateTimeStr: String): LocalDate? =
    try {
        LocalDate.parse(dateTimeStr.substringBefore("T"))
    } catch (_: DateTimeParseException) {
        null
    }

/**
 * Parses the time portion from an ISO 8601 date-time string.
 *
 * Extracts the part after "T" and delegates to [parseTime] for the
 * actual `HH:mm` parsing. Returns `null` if the string has no time
 * component or the time part is unparseable.
 *
 * This complements [parseIsoDate] for splitting schema.org `startDate`
 * values into separate date and time components.
 *
 * Example:
 * ```kotlin
 * parseIsoTime("2026-05-16T20:00")  // LocalTime.of(20, 0)
 * parseIsoTime("2026-05-16")        // null (no time component)
 * ```
 */
fun parseIsoTime(dateTimeStr: String): LocalTime? {
    val timePart = dateTimeStr.substringAfter("T", "")
    return parseTime(timePart)
}

/**
 * Parses a European short date in `DD/MM/YY` format.
 *
 * Two-digit years are resolved to the 2000–2099 range (e.g. "26" → 2026).
 * Single-digit day/month values are also accepted (e.g. "1/9/26").
 *
 * This format is used by some WordPress-based Berlin venue websites
 * (e.g. Madame Claude) for event dates. Returns `null` for unparseable input.
 *
 * Example:
 * ```kotlin
 * parseShortDate("21/09/26")  // LocalDate.of(2026, 9, 21)
 * parseShortDate("1/9/26")    // LocalDate.of(2026, 9, 1)
 * parseShortDate("invalid")   // null
 * ```
 */
fun parseShortDate(text: String?): LocalDate? {
    if (text.isNullOrBlank()) return null
    return try {
        LocalDate.parse(text.trim(), SHORT_DATE_FORMATTER)
    } catch (_: DateTimeParseException) {
        null
    }
}
