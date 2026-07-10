package de.norm.events.scraper

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [ScrapedEvent.toEventEntity], focused on the normalization it applies
 * at the persistence boundary. The exhaustive doors/start reordering cases live in
 * [EventMappingExtensionsTest]; here we only assert the mapping actually wires it in.
 */
class ScrapedEventTest {
    private fun scrapedEvent(
        doorsTime: LocalTime? = null,
        startTime: LocalTime? = null,
        title: String = "Berliner Weisse",
        eventType: String? = null
    ) = ScrapedEvent(
        title = title,
        eventType = eventType,
        eventDate = LocalDate.of(2026, 12, 30),
        sourceId = "so36:98223",
        sourceUrl = "https://www.so36.com/produkte/98223",
        doorsTime = doorsTime,
        startTime = startTime
    )

    private fun ScrapedEvent.toEntity() = toEventEntity(venueId = 1L, venueSlug = "so36", eventSourceId = 1L)

    @Test
    fun `toEventEntity swaps a transposed doors-after-start pair`() {
        // Source listed "Einlass: 19:30, Beginn: 19:00" — impossible, so the times are swapped back.
        val entity = scrapedEvent(doorsTime = LocalTime.of(19, 30), startTime = LocalTime.of(19, 0)).toEntity()

        entity.doorsTime shouldBe LocalTime.of(19, 0)
        entity.startTime shouldBe LocalTime.of(19, 30)
    }

    @Test
    fun `toEventEntity preserves an already-valid doors-start pair`() {
        val entity = scrapedEvent(doorsTime = LocalTime.of(19, 0), startTime = LocalTime.of(20, 0)).toEntity()

        entity.doorsTime shouldBe LocalTime.of(19, 0)
        entity.startTime shouldBe LocalTime.of(20, 0)
    }

    @Test
    fun `toEventEntity promotes an under-classified festival title to FESTIVAL`() {
        // Category-less "… Festival" (defaults to OTHER) and a "Konzert"-labelled festival day.
        scrapedEvent(title = "CANARIAS CALLING FESTIVAL").toEntity().eventType shouldBe "FESTIVAL"
        scrapedEvent(title = "GROSSSTADTWAHNSINN 2026 - FESTIVALTICKET", eventType = "CONCERT")
            .toEntity()
            .eventType shouldBe "FESTIVAL"
    }

    @Test
    fun `toEventEntity does not override an explicit non-festival type or a plain title`() {
        // A source that says PARTY is trusted even with "festival" in the title …
        scrapedEvent(title = "Freedom Festival Party", eventType = "PARTY").toEntity().eventType shouldBe "PARTY"
        // … and a plain concert title keeps its type.
        scrapedEvent(title = "Berliner Weisse", eventType = "CONCERT").toEntity().eventType shouldBe "CONCERT"
        scrapedEvent(title = "Manifest").toEntity().eventType shouldBe "OTHER"
    }
}
