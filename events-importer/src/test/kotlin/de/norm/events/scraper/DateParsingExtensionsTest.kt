package de.norm.events.scraper

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class DateParsingExtensionsTest {
    // --- parseTime ---

    @Test
    fun `parseTime parses valid HH-mm time`() {
        parseTime("19:00") shouldBe LocalTime.of(19, 0)
        parseTime("20:30") shouldBe LocalTime.of(20, 30)
        parseTime("00:00") shouldBe LocalTime.of(0, 0)
        parseTime("23:59") shouldBe LocalTime.of(23, 59)
    }

    @Test
    fun `parseTime trims whitespace`() {
        parseTime("  19:00  ") shouldBe LocalTime.of(19, 0)
        parseTime("\t20:30\n") shouldBe LocalTime.of(20, 30)
    }

    @Test
    fun `parseTime returns null for null input`() {
        parseTime(null).shouldBeNull()
    }

    @Test
    fun `parseTime returns null for blank input`() {
        parseTime("").shouldBeNull()
        parseTime("   ").shouldBeNull()
    }

    @Test
    fun `parseTime returns null for invalid format`() {
        parseTime("TBA").shouldBeNull()
        parseTime("19h00").shouldBeNull()
        parseTime("7pm").shouldBeNull()
        parseTime("25:00").shouldBeNull()
    }

    // --- parseIsoDate ---

    @Test
    fun `parseIsoDate parses full ISO datetime`() {
        parseIsoDate("2026-05-16T20:00") shouldBe LocalDate.of(2026, 5, 16)
    }

    @Test
    fun `parseIsoDate parses date-only string`() {
        parseIsoDate("2026-05-16") shouldBe LocalDate.of(2026, 5, 16)
    }

    @Test
    fun `parseIsoDate returns null for invalid input`() {
        parseIsoDate("invalid").shouldBeNull()
        parseIsoDate("16-05-2026").shouldBeNull()
        parseIsoDate("2026/05/16").shouldBeNull()
    }

    // --- parseIsoTime ---

    @Test
    fun `parseIsoTime parses time from full ISO datetime`() {
        parseIsoTime("2026-05-16T20:00") shouldBe LocalTime.of(20, 0)
        parseIsoTime("2026-05-16T19:30") shouldBe LocalTime.of(19, 30)
    }

    @Test
    fun `parseIsoTime returns null when no time component`() {
        parseIsoTime("2026-05-16").shouldBeNull()
    }

    @Test
    fun `parseIsoTime returns null for invalid time component`() {
        parseIsoTime("2026-05-16Tinvalid").shouldBeNull()
    }

    // --- parseShortDate ---

    @Test
    fun `parseShortDate parses DD-MM-YY format`() {
        parseShortDate("21/09/26") shouldBe LocalDate.of(2026, 9, 21)
    }

    @Test
    fun `parseShortDate parses single-digit day and month`() {
        parseShortDate("1/9/26") shouldBe LocalDate.of(2026, 9, 1)
    }

    @Test
    fun `parseShortDate trims whitespace`() {
        parseShortDate("  21/09/26  ") shouldBe LocalDate.of(2026, 9, 21)
    }

    @Test
    fun `parseShortDate returns null for null input`() {
        parseShortDate(null).shouldBeNull()
    }

    @Test
    fun `parseShortDate returns null for blank input`() {
        parseShortDate("").shouldBeNull()
        parseShortDate("   ").shouldBeNull()
    }

    @Test
    fun `parseShortDate returns null for invalid format`() {
        parseShortDate("invalid").shouldBeNull()
        parseShortDate("2026-09-21").shouldBeNull()
    }
}
